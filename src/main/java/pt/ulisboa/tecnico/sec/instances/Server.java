package pt.ulisboa.tecnico.sec.instances;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.AbstractMap.SimpleImmutableEntry;

import pt.ulisboa.tecnico.sec.blockchain.BlockchainNode;
import pt.ulisboa.tecnico.sec.blockchain.BlockchainState;
import pt.ulisboa.tecnico.sec.broadcasts.BestEffortBroadcast;
import pt.ulisboa.tecnico.sec.ibft.HDLProcess;
import pt.ulisboa.tecnico.sec.links.AuthenticatedPerfectLink;
import pt.ulisboa.tecnico.sec.links.Channel;
import pt.ulisboa.tecnico.sec.messages.BFTMessage;
import pt.ulisboa.tecnico.sec.messages.ClientRequestMessage;
import pt.ulisboa.tecnico.sec.messages.ClientResponseMessage;
import pt.ulisboa.tecnico.sec.messages.LinkMessage;
import pt.ulisboa.tecnico.sec.tes.TESState;
import pt.ulisboa.tecnico.sec.tes.transactions.Transaction;


@SuppressWarnings("unchecked")
public class Server extends HDLProcess {
	private boolean running = false;
	private boolean kys = true;
	private Channel channel;
	private BestEffortBroadcast ibftBroadcast;
	private List<SimpleImmutableEntry<Transaction, HDLProcess>> pendingRequests;
	private BlockchainState blockchainState;
	private BlockchainNode toPropose;
	private TESState tesState;

	// TES related variables
	private Consensus<BlockchainNode> consensus;
	private Object toProposeLock = new Object();
	private Map<PublicKey, Integer> clientsSeqNum;

	public Server(int id, int port) throws UnknownHostException {
		super(id, port);
		channel = new AuthenticatedPerfectLink(this);
		pendingRequests = new ArrayList<>();
		blockchainState = new BlockchainState();
		toPropose = new BlockchainNode();
		tesState = new TESState();
		clientsSeqNum = new HashMap<>();
	}

	protected Channel getChannel() {
		return this.channel;
	}

	public TESState getTESState() {
		return tesState;
	}

	public String getBlockChainState() {
		return blockchainState.toString();
	}

    public String getBlockChainStringRaw() {
        return blockchainState.getRaw();
    }

	public void execute() throws IllegalThreadStateException {
		ibftBroadcast = new BestEffortBroadcast(channel, InstanceManager.getAllParticipants());
		consensus = new Consensus<>(this, ibftBroadcast);

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
				System.out.println(e);
			}

			try {
				long ms = 5000; // Miliseconds to wait
				int ns = 1; // Nanoseconds to wait
				t.join(ms, ns);

				if (t.isAlive()) {
					System.out.println("Thread still alive even after waiting for " + ms / 1000 + ns / 1000000000 + " seconds...");

					System.out.println("/--- START OF STACK TRACE OF " + t.getId() + " ---\\");
					for (StackTraceElement ste : t.getStackTrace()) {
						System.out.println(ste);
					}
					System.out.println("\\--- END OF STACK TRACE OF " + t.getId() + " ---/");
				}
			} catch (InterruptedException e) {
				e.printStackTrace(System.out);
			}
		}

		this.selfTerminate();
		channel.close();

		System.out.printf("Server %d %s%n", this.getID(), tesState);

		System.out.printf("Server %d closed%n", this.getID());
	}

	private boolean checkTransactionNonce(Transaction t) {
		clientsSeqNum.putIfAbsent(t.getSource(), Integer.MIN_VALUE);
		int nonce = clientsSeqNum.get(t.getSource());

		if (t.getNonce() > nonce) clientsSeqNum.replace(t.getSource(), nonce, t.getNonce());
		else return false;

		return true;
	}

	private boolean verifyBlockChainNode(BlockchainNode node) {

		for (Transaction t : node.getTransactions().stream().sorted((x, y) -> x.getNonce() - y.getNonce()).toList()) {
			if (!t.checkSyntax() || !t.validateTransaction() || !checkTransactionNonce(t)) return false;
		}

		return true;
	}

	private void handleIncomingMessage(LinkMessage incomingMessage) throws InterruptedException {
		switch (incomingMessage.getMessage().getMessageType()) {
			case CLIENT_REQUEST:
				handleClientRequest(incomingMessage);
				break;
			case BFT:
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
				break;
			default:
				break;
		}
	}

	private void addTransactionToBlockchain(Transaction transaction) throws InterruptedException {
		BlockchainNode toProposeCopy = null;

		synchronized (toProposeLock) {
			toPropose.addTransaction(transaction, this.getPublicKey());
			if (toPropose.isFull()) {
				toProposeCopy = new BlockchainNode(toPropose.getTransactions(), toPropose.getRewards());
				toPropose = new BlockchainNode();
			}
		}

		if (toProposeCopy != null) {
			this.consensus.startConsensus(toProposeCopy);
		}
	}

	private void sendClientResponse(HDLProcess client, ClientResponseMessage.Status status, int instance) throws IllegalStateException, InterruptedException {
		ClientResponseMessage response = new ClientResponseMessage(status, instance);
		LinkMessage toSend = new LinkMessage(response, this, client);

		channel.send(toSend);
	}

	private void handleClientRequest(LinkMessage request) throws InterruptedException {
		ClientRequestMessage requestMessage = (ClientRequestMessage) request.getMessage();
		Transaction transaction = requestMessage.getTransaction();

		if (!transaction.validateTransaction() || !transaction.checkSyntax() || !checkTransactionNonce(transaction)) {
			sendClientResponse(request.getSender(), ClientResponseMessage.Status.REJECTED, -1);
			return;
		}

		pendingRequests.add(new SimpleImmutableEntry<>(transaction, request.getSender()));

		BlockchainNode toProposeCopy = new BlockchainNode(toPropose.getTransactions(), toPropose.getRewards());
		addTransactionToBlockchain(transaction);

		Thread.sleep(BlockchainNode.FILL_TIMEOUT);

		// After waiting for the timeout, check if block is still the same
		//  and if it is, add that block to the blockchain.
		boolean start = false;
		synchronized (toProposeLock) {
			if (toPropose.equals(toProposeCopy)) {
				start = true;
				toPropose = new BlockchainNode();
			}
		}
		if (start) {
			this.consensus.startConsensus(toProposeCopy);
		}
	}

	private void decide(BFTMessage<BlockchainNode> message) throws InterruptedException {
		BlockchainNode block = message.getValue();

		for (int j = 0; j < block.getTransactions().size(); j++) {
			Transaction transaction = block.getTransactions().get(j);
			// Perform transaction (in a whole)
			boolean successfulTransaction = transaction.updateTESState(tesState);

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
			System.out.printf("Server %d deciding for client %s with proposed value %s at instance %d%n", this.getID(), client, block, message.getInstance());

			ClientResponseMessage.Status status = successfulTransaction ? ClientResponseMessage.Status.OK : ClientResponseMessage.Status.REJECTED;

			new Thread(() -> {
				try {
					sendClientResponse(client, status, message.getInstance());
				} catch (IllegalStateException | InterruptedException e) {
					e.printStackTrace();
				}
			}).start();
		}

		for (Transaction t : block.getRewards()) {
			t.updateTESState(tesState);
			// FIXME: maybe just add balance to leader's account?
			// tesState.getAccount(getPublicKey()).addBalance(BlockchainNode.TRANSACTION_FEE);
			// TESAccount source = tesState.getAccount(t.getSource());
			// if (source != null) source.subtractBalance(BlockchainNode.TRANSACTION_FEE);
		}

		blockchainState.append(message.getInstance(), block);
	}

	public void kill() {
		this.kys = true;
	}
}
