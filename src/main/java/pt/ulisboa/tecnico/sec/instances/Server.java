package pt.ulisboa.tecnico.sec.instances;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.sec.blockchain.BlockchainNode;
import pt.ulisboa.tecnico.sec.blockchain.BlockchainState;
import pt.ulisboa.tecnico.sec.broadcasts.BestEffortBroadcast;
import pt.ulisboa.tecnico.sec.ibft.HDLProcess;
import pt.ulisboa.tecnico.sec.links.AuthenticatedPerfectLink;
import pt.ulisboa.tecnico.sec.links.Channel;
import pt.ulisboa.tecnico.sec.messages.BFTMessage;
import pt.ulisboa.tecnico.sec.messages.CheckBalanceResponseMessage;
import pt.ulisboa.tecnico.sec.messages.ClientRequestMessage;
import pt.ulisboa.tecnico.sec.messages.ClientResponseMessage;
import pt.ulisboa.tecnico.sec.messages.LinkMessage;
import pt.ulisboa.tecnico.sec.messages.PropagateChangesMessage;
import pt.ulisboa.tecnico.sec.tes.SignedTESAccount;
import pt.ulisboa.tecnico.sec.tes.TESAccount;
import pt.ulisboa.tecnico.sec.tes.TESState;
import pt.ulisboa.tecnico.sec.tes.transactions.CheckBalanceTransaction;
import pt.ulisboa.tecnico.sec.tes.transactions.Transaction;
import pt.ulisboa.tecnico.sec.tes.transactions.TransferTransaction;


@SuppressWarnings("unchecked")
public class Server extends ByzantineProcess {
	private static final Integer SNAPSHOT_BLOCK_SIZE = 3;

    // FLAG FOR BYZANTINE BEHAVIOUR
    private boolean isByzantine = false;

	private boolean running = false;
	private boolean kys = true;
	private Channel channel;
	private BestEffortBroadcast ibftBroadcast;
	private List<SimpleImmutableEntry<Transaction, HDLProcess>> pendingRequests;
	private BlockchainState blockchainState;
	private BlockchainNode toPropose;
	private Map<Integer, TESState> tesStates;

	// TES related variables
	private Consensus<BlockchainNode> consensus;
	private Consensus<StrongReadIBFTValue> readConsensus;
	private Object toProposeLock = new Object();
	private Map<PublicKey, Integer> clientsSeqNum;
	private Integer snapshotCounter = 0;
	private List<Transaction> snapshotTransaction;

	// For each block, keep a collection of signed states for each account.
	// This collection is a set so that no attacker can send multiple of the same state.
	private Map<Integer, Map<PublicKey, Set<SignedTESAccount>>> snapshots;

    public Server(int id, int port, boolean isByzantine) throws UnknownHostException {
        this(id, port);
        this.isByzantine = isByzantine;
    }

	public Server(int id, int port) throws UnknownHostException {
		super(id, port);
		channel = new AuthenticatedPerfectLink(this);
		pendingRequests = new ArrayList<>();
		blockchainState = new BlockchainState();
		toPropose = new BlockchainNode();
		tesStates = new ConcurrentHashMap<>();
		clientsSeqNum = new ConcurrentHashMap<>();
		snapshots = new ConcurrentHashMap<>();
		snapshotTransaction = new ArrayList<>();

		tesStates.put(-1, new TESState());
	}

    public boolean isByzantine() {
        return this.isByzantine;
    }

    public void setByzantine() {
        System.out.printf("[%d] --------- I'm BYZANTINE WOOOOOOOO%n", this._id);
        setByzantine(true);
    }

    public void setByzantine(boolean isByzantine) {
        this.isByzantine = isByzantine;
    }

	protected Channel getChannel() {
		return this.channel;
	}

	public TESState getTESState(int timestamp) {
		return tesStates.getOrDefault(timestamp, null);
	}

	public Map<Integer, TESState> getTESStates() {
		return tesStates;
	}

	public int getLastTimestamp() {
		return tesStates.entrySet().stream().reduce((a, b) -> b.getKey() > a.getKey() ? b : a).get().getKey();
	}

	public TESState getLastTESState() {
		return getTESState(getLastTimestamp());
	}

	public String getBlockChainState() {
		return blockchainState.toString();
	}

    public String getBlockChainStringRaw() {
        return blockchainState.getRaw();
    }

    public void executeByzantineBehaviour(ByzantineBehaviour behaviour) {
		System.out.printf("[%d] --------- Just got byzantine behaviour " + behaviour + ".%n", this._id);
        switch (behaviour) {
			case TERMINATE:
				stopByzantineBehaviour();
				running = false;
				System.out.printf("[%d] --------- Im no longer byzantine :((((  ( i still am, just quitting because i'm cool :sunglasses: )%n", this._id);
				break;
			case INCOMPLETE_BROADCAST:
				List<HDLProcess> broadcastProcs = ibftBroadcast.getInteractProcesses();
				Collections.shuffle(broadcastProcs);
				ibftBroadcast.setInteractProcesses(broadcastProcs.subList(0, getRandomGenerator().nextInt(broadcastProcs.size())));
				
				System.out.printf("[%d] --------- Broadcast sabotaged successfully, N=%d.%n", this._id, ibftBroadcast.getInteractProcesses().size());
				try {
					//Thread.sleep(this.TIMEOUT / 2);
				} catch (Exception e) {
					e.printStackTrace();
				}
				//ibftBroadcast.setInteractProcesses(InstanceManager.getAllParticipants());
				break;
			default:
				break;
		}
    }

	public void execute() throws IllegalThreadStateException {
		ibftBroadcast = new BestEffortBroadcast(channel, InstanceManager.getAllParticipants());
		consensus = new Consensus<>(this, ibftBroadcast);
		readConsensus = new Consensus<>(this, ibftBroadcast);

        if (isByzantine) startByzantineBehaviour();

		this.running = true;
		this.kys = false;
		List<Thread> activeHandlerThreads = new ArrayList<>();

		// Wait for client packets
		while (running) {
			System.err.printf("Server %s waiting for some request from a client...%n", this);
			try {
				// Receives message
				LinkMessage requestMessage = ibftBroadcast.deliver();

				new Thread(() ->{
					synchronized (activeHandlerThreads) {
						activeHandlerThreads.add(Thread.currentThread());
					}
					try {
						handleIncomingMessage(requestMessage);
					} catch (IllegalStateException | NullPointerException | InterruptedException e) {
						e.printStackTrace();
						System.err.printf("Server %d %s catch %s%n", this.getID(), Thread.currentThread().getName(), e.toString());
					} finally {
						synchronized (activeHandlerThreads) {
							activeHandlerThreads.remove(Thread.currentThread());
						}
					}
				}).start();
			} catch (SocketTimeoutException e) {
				System.err.println("Socket waited for too long, maybe no more messages?");
				if (this.kys) {
					this.running = false;
				}
				continue;
			} catch (IllegalStateException | InterruptedException | NullPointerException e) {
				System.err.printf("Server %d catch %s%n", this.getID(), e.toString());
				continue;
			}
		}

		System.err.printf("Server %d is closing...%n", this.getID());

		for (int i = 0; i < activeHandlerThreads.size(); i++) {
			Thread t = null;
			synchronized (activeHandlerThreads) {
				t = activeHandlerThreads.get(i);
			}

			try {
				t.interrupt();
			} catch (Exception e) {
				System.err.println(e);
			}

			try {
				long ms = 5000; // Miliseconds to wait
				int ns = 1; // Nanoseconds to wait
				t.join(ms, ns);

				if (t.isAlive()) {
					System.err.println("Thread still alive even after waiting for " + ms / 1000 + ns / 1000000000 + " seconds...");

					System.err.println("/--- START OF STACK TRACE OF " + t.getId() + " ---\\");
					for (StackTraceElement ste : t.getStackTrace()) {
						System.err.println(ste);
					}
					System.err.println("\\--- END OF STACK TRACE OF " + t.getId() + " ---/");
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		this.selfTerminate();
		channel.close();

		if (!this.isByzantine)
			System.out.printf("Server %d %s%n", this.getID(), getLastTESState());

		System.out.printf("Server %d closed%n", this.getID());
	}

	private boolean checkTransactionNonce(Transaction t) {
		clientsSeqNum.putIfAbsent(t.getSource(), Integer.MIN_VALUE);
		System.out.println("[" + this._id + " | " + t +"] CHECKING TRANSACTION");
		System.out.println("[" + this._id + " | " + t +"] source == null ? " + (t.getSource() == null));
		System.out.println("[" + this._id + " | " + t +"] clientsSeqNum == null ? " + (clientsSeqNum == null));
		System.out.println("[" + this._id + " | " + t +"] clientsSeqNum has source ? " + clientsSeqNum.containsKey(t.getSource()));
		int nonce = clientsSeqNum.get(t.getSource());

		if (t.getNonce() > nonce) clientsSeqNum.replace(t.getSource(), nonce, t.getNonce());
		else return false;

		return true;
	}

	private boolean verifyBlockChainNode(BlockchainNode node) {

		for (Transaction t : node.getTransactions().stream().sorted((x, y) -> x.getNonce() - y.getNonce()).collect(Collectors.toList())) {
			if (!t.checkSyntax() || !t.validateTransaction() || !checkTransactionNonce(t)) return false;
		}

		return true;
	}

	private void handleBFTMessage(LinkMessage incomingMessage) throws InterruptedException {
		BFTMessage<?> message = (BFTMessage<?>) incomingMessage.getMessage();
		if (message.getClazz() == BlockchainNode.class) {
			handleBFTMessageBlockchain(incomingMessage);
		} else {
			handleBFTMessageStrongRead(incomingMessage);
		}
	}

	private void handleBFTMessageStrongRead(LinkMessage incomingMessage) throws InterruptedException, IllegalStateException {
		BFTMessage<StrongReadIBFTValue> message = readConsensus.handleCommit(incomingMessage);
		if (message == null) return;

		System.err.println("Server " + this.getID() + " returning strong read to client.");

		StrongReadIBFTValue value = message.getValue();

		Integer timestamp = value.getTimestamp();
		Integer nonce = value.getNonce();
		PublicKey clientKey = value.getClientKey();
		HDLProcess client = InstanceManager.getHDLProcess(clientKey);

		if (client == null) return;

		TESState state = getTESState(timestamp);
		if (state == null) return;

		TESAccount account = state.getAccount(clientKey);
		if (account == null) {
			// Client doesn't have an account yet :(
			sendClientResponse(client, ClientResponseMessage.Status.NOT_FOUND, timestamp, nonce);
			return;
		}

		double tucs = account.getTucs();

		CheckBalanceResponseMessage response = new CheckBalanceResponseMessage(ClientResponseMessage.Status.OK, timestamp, nonce, tucs);
		System.err.printf("Server %d sending strong read response (%s) to client %d%n", this.getID(), response, client.getID());
		sendClientResponse(client, response);
	}

	private void handleBFTMessageBlockchain(LinkMessage incomingMessage) throws InterruptedException {
		BFTMessage<BlockchainNode> message = (BFTMessage<BlockchainNode>) incomingMessage.getMessage();
		switch (message.getType()) {
			case PRE_PREPARE:
				verifyBlockChainNode(message.getValue());
				this.consensus.handlePrePrepare(incomingMessage);
				break;
			case PREPARE:
				this.consensus.handlePrepare(incomingMessage);
				break;
			case COMMIT:
				BFTMessage<BlockchainNode> commitResult = this.consensus.handleCommit(incomingMessage);
				if (commitResult != null) {
					decide(commitResult);
				}
				break;
		}
	}

	private void handleIncomingMessage(LinkMessage incomingMessage) throws InterruptedException {
		switch (incomingMessage.getMessage().getMessageType()) {
			case CLIENT_REQUEST:
				handleClientRequest(incomingMessage);

				break;
			case BFT:
				handleBFTMessage(incomingMessage);

				break;
			case PROPAGATE_CHANGES:
				handleChangesPropagation(incomingMessage);

				break;
			default:
				break;
		}
	}

	public void handleChangesPropagation(LinkMessage message) {
		PropagateChangesMessage propagateMessage = (PropagateChangesMessage) message.getMessage();
		PublicKey senderKey = message.getSender().getPublicKey();

		System.err.printf("Server %d received account updates: %s%n", this.getID(), propagateMessage.toString());

		snapshots.putIfAbsent(propagateMessage.getTimestamp(), new HashMap<>());
		Map<PublicKey, Set<SignedTESAccount>> timestampMap = snapshots.get(propagateMessage.getTimestamp());

		for (SignedTESAccount state : propagateMessage.getChanges()) {
			if (state.validateState(senderKey)) {
				timestampMap.putIfAbsent(state.getOwner(), new HashSet<>());
				Set<SignedTESAccount> signedStates = timestampMap.get(state.getOwner());
				signedStates.add(state);
			}
		}
	}

	public Set<SignedTESAccount> weaklyConsistentRead(PublicKey accountKey) {
		// snapshots ordered from most recent timestamp to older timestamp
		List<Map<PublicKey, Set<SignedTESAccount>>> sortedList = snapshots.entrySet()
			.stream()
			.sorted(Map.Entry.<Integer, Map<PublicKey, Set<SignedTESAccount>>>comparingByKey().reversed())
			.map(Map.Entry::getValue)
			.collect(Collectors.toList());

		for (Map<PublicKey, Set<SignedTESAccount>> timestampMap : sortedList) {
			// if account wasn't updated in this timestamp or there aren't enough tokens, skip to older timestamp
			if (timestampMap.get(accountKey) == null || timestampMap.get(accountKey).size() < InstanceManager.getNumberOfByzantines() + 1) continue;

			// verify that there are f+1 equal states
			Map<Double, Integer> tucsAmountCounter = new HashMap<>();

			for (SignedTESAccount state : timestampMap.get(accountKey)) {
				tucsAmountCounter.putIfAbsent(state.getBalance(), 0);
				int count = tucsAmountCounter.get(state.getBalance()) + 1;
				tucsAmountCounter.replace(state.getBalance(), count);

				if (count == InstanceManager.getNumberOfByzantines() + 1) {
					return timestampMap.get(accountKey);
				}
			}
		}

		return new HashSet<>();
	}

	private void addTransactionToBlockchain(Transaction transaction) throws InterruptedException {
		BlockchainNode toProposeCopy = null;

		synchronized (toProposeLock) {
			System.out.printf("[%d] Hi im adding %s to %s%n", this._id, transaction, toPropose);
			toPropose.addTransaction(transaction, this.getPublicKey());
			System.out.printf("[%d] Hi i just added %s to %s%n", this._id, transaction, toPropose);
			if (toPropose.isFull()) {
				toProposeCopy = new BlockchainNode(toPropose.getTransactions(), toPropose.getRewards());
				toPropose = new BlockchainNode();
			}
		}

		if (toProposeCopy != null) {
			this.consensus.startConsensus(toProposeCopy);
		}
	}

	private void sendClientResponse(HDLProcess client, ClientResponseMessage message) throws IllegalStateException, InterruptedException {
		LinkMessage toSend = new LinkMessage(message, this, client);

		channel.send(toSend);
	}

	private void sendClientResponse(HDLProcess client, ClientResponseMessage.Status status, int instance, int nonce) throws IllegalStateException, InterruptedException {
		sendClientResponse(client, new ClientResponseMessage(status, instance, nonce));
	}

	private void handleClientRequest(LinkMessage request) throws InterruptedException {
		ClientRequestMessage requestMessage = (ClientRequestMessage) request.getMessage();
		Transaction transaction = requestMessage.getTransaction();

		if (transaction.getOperation().equals(Transaction.TESOperation.CHECK_BALANCE)) {
			CheckBalanceTransaction readTransaction = (CheckBalanceTransaction) transaction;

			if (readTransaction.getReadType().equals(CheckBalanceTransaction.ReadType.WEAKLY_CONSISTENT)) {
				Set<SignedTESAccount> signedStates = weaklyConsistentRead(readTransaction.getOwner());
				ClientResponseMessage.Status status = ClientResponseMessage.Status.NOT_FOUND;
				int instance = -1;

				System.err.printf("Server %d handling weak reads for client %s%n", this._id, request.getSender());

				if (!signedStates.isEmpty()) {
					status = ClientResponseMessage.Status.OK;
					instance = 0;
				}

				sendClientResponse(request.getSender(), new CheckBalanceResponseMessage(status, instance, transaction.getNonce(), signedStates));
				// sendClientResponse(request.getSender(), status, instance, transaction.getNonce());
			}
			else {
				System.err.printf("Server %d handling strong reads for client %s%n", this._id, request.getSender());
				int instance = readConsensus.incrementInstance();
				StrongReadIBFTValue value = new StrongReadIBFTValue(getLastTimestamp(), transaction.getSource(), transaction.getNonce());
				BFTMessage<StrongReadIBFTValue> commit = new BFTMessage<>(BFTMessage.Type.COMMIT, instance, readConsensus.getRound(), value);
				
				System.err.printf("Server %d broadcasting BFT message with strong read value %s%n", this._id, commit.getValue());
				ibftBroadcast.broadcast(commit);
			}

			return;
		}

		try {
		System.out.printf("Server %d validating request from client %d.%n", this._id, request.getSender().getID()); // epic amogus fail tava no err ;-;

		if (!transaction.validateTransaction() || !transaction.checkSyntax() || !checkTransactionNonce(transaction)) {
			sendClientResponse(request.getSender(), ClientResponseMessage.Status.REJECTED, -1, transaction.getNonce());
			System.out.printf("Server %d rejecting transaction %s, as it is invalid.%n", this._id, transaction);
			return;
		}

		pendingRequests.add(new SimpleImmutableEntry<>(transaction, request.getSender()));

		BlockchainNode toProposeCopy = BlockchainNode.copy(toPropose);
		System.out.printf("[%d] Before adding %s copy of toPropose is %s%n", this._id, transaction, toProposeCopy);
		addTransactionToBlockchain(transaction);

		long time = System.currentTimeMillis();
		System.out.printf("[%d] Starting block timeout for %s%n", this._id, transaction);
		Thread.sleep(BlockchainNode.FILL_TIMEOUT);
		long newTime = System.currentTimeMillis();
		System.out.printf("[%d] Block timeout reached for %s, %d, %d.%n", this._id, transaction, newTime, newTime - time);

		// After waiting for the timeout, check if block is still the same
		//  and if it is, add that block to the blockchain.
		boolean start = false;
		synchronized (toProposeLock) {
			System.out.printf("[%d] Block timeout reached, toPropose = %s and copy = %s.%n", this._id, toPropose, toProposeCopy);
			if (toPropose.equals(toProposeCopy)) {
				System.out.printf("[%d] Block timeout reached AND toPropose didn't change.%n", this._id);
				start = true;
				toPropose = new BlockchainNode();
			}
		}
		if (start) {
			this.consensus.startConsensus(toProposeCopy);
		}
		} catch (Exception e) {
			e.printStackTrace(System.out);
			System.out.flush();
			throw e;
		}
	}

	private void decide(BFTMessage<BlockchainNode> message) throws InterruptedException {
		BlockchainNode block = message.getValue();
		int timestamp = message.getInstance();
		tesStates.putIfAbsent(timestamp, tesStates.get(timestamp-1).copy());
		TESState currentState = tesStates.get(timestamp);
		// we assume that decides are in order (if not: kabooom)

		for (int j = 0; j < block.getTransactions().size(); j++) {
			Transaction transaction = block.getTransactions().get(j);
			// Perform transaction (in a whole)
			boolean successfulTransaction = transaction.updateTESState(currentState);

			if (successfulTransaction) snapshotTransaction.add(transaction);

			// Lookup for the source of the transaction
			int idx = -1;
			for (int i = pendingRequests.size() - 1; i >= 0; i--) {
				if (pendingRequests.get(i).getKey().equals(transaction)) {
					idx = i;
					break;
				}
			}
			if (idx == -1) {
				System.err.printf("Server %d request %s was lost %n", this.getID(), transaction);
				continue;
			}

			// Sending response to the client
			HDLProcess client = pendingRequests.remove(idx).getValue();
			System.err.printf("Server %d deciding for client %s with proposed value %s at instance %d%n", this.getID(), client, block, message.getInstance());

			ClientResponseMessage.Status status = successfulTransaction ? ClientResponseMessage.Status.OK : ClientResponseMessage.Status.REJECTED;

			new Thread(() -> {
				try {
					sendClientResponse(client, status, message.getInstance(), transaction.getNonce());
				} catch (IllegalStateException | InterruptedException e) {
					e.printStackTrace();
				}
			}).start();
		}

		for (Transaction t : block.getRewards()) {
			if (t.updateTESState(currentState))
			snapshotTransaction.add(t);
		}

		blockchainState.append(message.getInstance(), block);

		if (snapshotCounter++ % SNAPSHOT_BLOCK_SIZE == 0) {
			propagateSignedChanges(message.getInstance(), snapshotTransaction);
			snapshotTransaction.clear();
		}
		snapshotCounter %= SNAPSHOT_BLOCK_SIZE;
	}

	private void propagateSignedChanges(int timestamp, List<Transaction> transactions) throws IllegalStateException, InterruptedException {
		Set<PublicKey> updatedAccounts = new HashSet<>();

		for (Transaction t : transactions) {
			updatedAccounts.add(t.getSource());
			if (t.getOperation().equals(Transaction.TESOperation.TRANSFER))
				updatedAccounts.add(((TransferTransaction) t).getDestination());
		}
		System.err.printf("Server %d propagating states for %d accounts.%n", this._id, updatedAccounts.size());

		PropagateChangesMessage message = new PropagateChangesMessage(timestamp);

		for (PublicKey key : updatedAccounts) {
			TESAccount account = tesStates.get(timestamp).getAccount(key);
			SignedTESAccount accountState = new SignedTESAccount(account);
			accountState.authenticateState(this.getPublicKey(), this.getPrivateKey());
			message.addAccount(accountState);
		}

		ibftBroadcast.broadcast(message);
	}

	public void kill() {
		this.kys = true;
	}
}
