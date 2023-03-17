package pt.ulisboa.tecnico.sec.instances;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap.SimpleImmutableEntry;

import pt.ulisboa.tecnico.sec.broadcasts.BestEffortBroadcast;
import pt.ulisboa.tecnico.sec.ibft.BlockchainState;
import pt.ulisboa.tecnico.sec.ibft.BlockchainNode;
import pt.ulisboa.tecnico.sec.ibft.HDLProcess;
import pt.ulisboa.tecnico.sec.links.AuthenticatedPerfectLink;
import pt.ulisboa.tecnico.sec.messages.BFTMessage;
import pt.ulisboa.tecnico.sec.messages.ClientRequestMessage;
import pt.ulisboa.tecnico.sec.messages.ClientResponseMessage;
import pt.ulisboa.tecnico.sec.messages.LinkMessage;


public class Server extends HDLProcess {
	private boolean running = false;
	private boolean kys = true;
	private AuthenticatedPerfectLink channel;
	private BestEffortBroadcast ibftBroadcast;
	private List<SimpleImmutableEntry<BlockchainNode, HDLProcess>> pendingRequests;
	private BlockchainState blockchainState;

	// IBFT related variables
	private int instance = 0;
	private Object instanceLock = new Object();
	private int round;
	private int prepared_round;
	private BlockchainNode prepared_value;
	private Map<BFTMessage, Set<Integer>> prepareCount;
	private Map<BFTMessage, Set<Integer>> commitCount;


	public Server(int id, int port) throws UnknownHostException {
		super(id, port);
		channel = new AuthenticatedPerfectLink(this);
		pendingRequests = new ArrayList<>();
		prepareCount = new HashMap<>();
		commitCount = new HashMap<>();
		blockchainState = new BlockchainState();
	}

	public String getBlockChainState() {
		return blockchainState.toString();
	}

	public void execute() {
		ibftBroadcast = new BestEffortBroadcast(channel, InstanceManager.getServerProcesses());

		this.running = true;
		this.kys = false;
		List<Thread> activeHandlerThreads = new ArrayList<>();

		// Wait for client packets
		while (running) {
			System.out.printf("Server %s waiting for some request from a client...%n", this);
			try {
				// Receives message
				LinkMessage requestMessage = ibftBroadcast.deliver();

				new Thread(() ->{
					synchronized (activeHandlerThreads) {
						activeHandlerThreads.add(Thread.currentThread());
					}
					try {
						handleIncomingMessage(requestMessage);
					} catch (IllegalStateException | NullPointerException e) {
						System.err.printf("Server %d %s catch %s%n", this.getID(), Thread.currentThread().getName(), e.toString());
					} catch (InterruptedException e) {
						e.printStackTrace(System.out);
					} finally {
						synchronized (activeHandlerThreads) {
							activeHandlerThreads.remove(Thread.currentThread());
						}
					}
				}).start();
			} catch (SocketTimeoutException e) {
				System.out.println("Socket waited for too long, maybe no more messages?");
				if (this.kys) {
					this.running = false;
				}
				continue;
			} catch (IllegalStateException | InterruptedException | NullPointerException e) {
				System.err.printf("Server %d catch %s%n", this.getID(), e.toString());
				continue;
			}
		}

		System.err.printf("Server %d is closing...\n", this.getID());

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

		System.out.printf("Server %d closed\n", this.getID());
	}


	private void handleIncomingMessage(LinkMessage incomingMessage) throws InterruptedException {
		switch (incomingMessage.getMessage().getMessageType()) {
			case CLIENT_REQUEST:
				handleClientRequest(incomingMessage);
				break;
			case BFT:
				switch(((BFTMessage) incomingMessage.getMessage()).getType()) {
					case PRE_PREPARE:
						handlePrePrepare(incomingMessage);
						break;
					case PREPARE:
						handlePrepare(incomingMessage);
						break;
					case COMMIT:
						handleCommit(incomingMessage);
						break;
					case ROUND_CHANGE:
						handleRoundChange(incomingMessage);
						break;
				}
				break;
			default:
				break;
		}
	}

	// Start IBFT protocol if this process is the leader
	private void startConsensus(BlockchainNode value) throws InterruptedException {
		int currentInstance = 0;
		synchronized (instanceLock) {
			currentInstance = instance++;
		}
		if (this.equals(InstanceManager.getLeader(currentInstance, 0))) {
			System.out.printf("Server %d starting instance %d of consensus %n", this.getID(), this.instance);
			BFTMessage pre_prepare = new BFTMessage(BFTMessage.Type.PRE_PREPARE, currentInstance, 0, value);
			System.out.printf("Leader -> Pre-Prepare instance %d%n", pre_prepare.getInstance());
			ibftBroadcast.broadcast(pre_prepare);
		}
	}

	private void handleClientRequest(LinkMessage request) throws InterruptedException {
		ClientRequestMessage requestMessage = (ClientRequestMessage) request.getMessage();

		BlockchainNode value = new BlockchainNode(request.getSender().getID(), requestMessage.getValue());

		pendingRequests.add(new SimpleImmutableEntry<>(value, request.getSender()));

		startConsensus(value);
	}

	private void handlePrePrepare(LinkMessage pre_prepare) throws InterruptedException {
		int currentInstance;
		synchronized (instanceLock) {
			currentInstance = instance;
		}
		if (!InstanceManager.getLeader(currentInstance, round).equals(pre_prepare.getSender()))
			return;

		BFTMessage message = (BFTMessage) pre_prepare.getMessage();

		if (pre_prepare.getSender().equals(InstanceManager.getLeader(message.getInstance(), message.getRound()))) {
			System.out.printf("Server %d received valid pre-prepare from %d of consensus %d %n", this.getID(), pre_prepare.getSender().getID(), message.getInstance());
			BFTMessage toBroadcast = new BFTMessage(BFTMessage.Type.PREPARE, message.getInstance(), message.getRound(), message.getValue());
			ibftBroadcast.broadcast(toBroadcast);
		}
	}

	private void handlePrepare(LinkMessage prepare) throws InterruptedException {
		BFTMessage message = (BFTMessage) prepare.getMessage();

		System.out.printf("Server %d received valid prepare from %d of consensus %d %n", this.getID(), prepare.getSender().getID(), message.getInstance());

		int count = 0;
		synchronized (prepareCount) {
			prepareCount.putIfAbsent(message, new HashSet<>());
			prepareCount.get(message).add(prepare.getSender().getID());
			count = prepareCount.get(message).size() + 1;
		}

		if (count == InstanceManager.getQuorum()) {
			System.out.printf("Server %d received valid prepare quorum of consensus %d %n", this.getID(), message.getInstance());
			prepared_round = message.getRound();
			prepared_value = message.getValue();
			BFTMessage toBroadcast = new BFTMessage(BFTMessage.Type.COMMIT, message.getInstance(), message.getRound(), message.getValue());
			ibftBroadcast.broadcast(toBroadcast);
		}
	}

	private void handleCommit(LinkMessage commit) throws InterruptedException {
		BFTMessage message = (BFTMessage) commit.getMessage();

		int count = 0;
		synchronized (commitCount) {
			commitCount.putIfAbsent(message, new HashSet<>());
			commitCount.get(message).add(commit.getSender().getID());
			count = commitCount.get(message).size() + 1;
		}

		if (count == InstanceManager.getQuorum()) {
			System.out.printf("Server %d received valid commit quorum of consensus %d with value %s %n", this.getID(), message.getInstance(), message.getValue());
			decide(message);
		}
	}

	private void handleRoundChange(LinkMessage round_change) throws InterruptedException {
		// not needed for now -> stage 2
	}

	private void decide(BFTMessage message) throws InterruptedException {
		// Maybe useful for 2nd stage?
		// if (!this.equals(InstanceManager.getLeader(message.getInstance(), message.getRound()))) instance++;
		blockchainState.append(message.getInstance(), message.getValue());
		int idx = -1;
		for (int i = pendingRequests.size()-1; i >= 0; i--) {
			if (pendingRequests.get(i).getKey().equals(message.getValue())) {
				idx = i;
				break;
			}
		}
		if (idx == -1) return;
		HDLProcess client = pendingRequests.remove(idx).getValue();
		ClientResponseMessage response = new ClientResponseMessage(ClientResponseMessage.Status.OK, message.getInstance());
		LinkMessage toSend = new LinkMessage(response, this, client);
		channel.send(toSend);
		if (!pendingRequests.isEmpty()) {
			startConsensus(pendingRequests.get(0).getKey());
		}
	}

	public void kill() {
		this.kys = true;
		/*try {
			ClientRequestMessage dummy = new ClientRequestMessage("KYS (in-game)");
			LinkMessage killMessage = new LinkMessage(dummy, this, this, true);
			System.out.printf("kilelele for %d%n", this.getID());
			channel.send(killMessage);
		}
		catch (IllegalStateException | InterruptedException ile) {
			System.err.printf("Tried to kill server %d but channel was already closed or receiver terminated%n", this.getID());
		}*/
	}
}
