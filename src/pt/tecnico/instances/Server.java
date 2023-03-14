package pt.tecnico.instances;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.tecnico.broadcasts.BestEffortBroadcast;
import pt.tecnico.ibft.BlockchainState;
import pt.tecnico.ibft.HDLProcess;
import pt.tecnico.links.AuthenticatedPerfectLink;
import pt.tecnico.messages.BFTMessage;
import pt.tecnico.messages.ClientMessage;
import pt.tecnico.messages.LinkMessage;


public class Server extends HDLProcess {
	private boolean running = false;
	private AuthenticatedPerfectLink channel;
	private BestEffortBroadcast ibftBroadcast;
	private Map<String, HDLProcess> pendingRequests;
	private BlockchainState blockchainState;

	// IBFT related variables
	private int instance = 0;
	private int round;
	private int prepared_round;
	private String prepared_value;
	private Map<BFTMessage, Integer> prepareCount;
	private Map<BFTMessage, Integer> commitCount;


	public Server(int id, int port) throws UnknownHostException {
		super(id, port);
		channel = new AuthenticatedPerfectLink(this);
		pendingRequests = new HashMap<>();
		prepareCount = new HashMap<>();
		commitCount = new HashMap<>();
		blockchainState = new BlockchainState("");

		ibftBroadcast = new BestEffortBroadcast(channel, InstanceManager.getServerProcesses());
	}

	public void execute() {
		System.out.printf("Server %d will receive messages on port %d \n", this.getID(), this.getPort());

		this.running = true;
		boolean terminateMsgSeen = false;

		// Wait for client packets
		while (running || !terminateMsgSeen) {
			System.out.printf("Server " + this.getID() + " Waiting for some request from a client...\n");
			try {
				// Receives message
				LinkMessage requestMessage = channel.deliver();

				if (requestMessage.getTerminate()) {
					System.err.printf("Server %d saw terminate\n", this.getID());
					terminateMsgSeen = true;
					continue;
				}

				new Thread(() ->{
					handleIncomingMessage(requestMessage);
				}).start();
			} catch (IllegalStateException | InterruptedException e) {
				System.err.printf("Server %d catch %s\n", this.getID(), e.toString());
				continue;
			}
		}

		System.err.printf("Server %d is closing...\n", this.getID());
		
		this.selfTerminate();
		channel.close();

		System.out.printf("Server %d closed\n", this.getID());
	}

	private void handleIncomingMessage(LinkMessage incomingMessage) {
		switch(incomingMessage.getMessage().getMessageType()) {
			case CLIENT:
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

	private void handleClientRequest(LinkMessage request) {
		// TODO : this is a barebones method, how to handle instance change??

		ClientMessage requestMessage = (ClientMessage) request.getMessage();

		pendingRequests.put(requestMessage.getValue(), request.getSender());

		if (this.getID() == InstanceManager.getLeader(instance, 0).getID()) {
			System.out.printf("Server %d starting instance %d of consensus %n", this.getID(), this.instance);
			BFTMessage pre_prepare = new BFTMessage(BFTMessage.Type.PRE_PREPARE, instance, 0, requestMessage.getValue());
			ibftBroadcast.broadcast(pre_prepare);
		}
	}

	private void handlePrePrepare(LinkMessage pre_prepare) {
		BFTMessage message = (BFTMessage) pre_prepare.getMessage();

		System.out.printf("Server %d recieved pre-prepare from %d of consensus %d %n", this.getID(), pre_prepare.getSender().getID(), message.getInstance());

		if (pre_prepare.getSender().getID() == InstanceManager.getLeader(message.getInstance(), message.getRound()).getID()) {
			System.out.printf("Server %d recieved valid pre-prepare from %d of consensus %d %n", this.getID(), pre_prepare.getSender().getID(), message.getInstance());
			BFTMessage toBroadcast = new BFTMessage(BFTMessage.Type.PREPARE, message.getInstance(), message.getRound(), message.getValue());
			ibftBroadcast.broadcast(toBroadcast);
		}
	}

	private void handlePrepare(LinkMessage prepare) {
		BFTMessage message = (BFTMessage) prepare.getMessage();

		synchronized(prepareCount) {
			prepareCount.putIfAbsent(message, 0);

			int count = prepareCount.get(message);

			prepareCount.put(message, count + 1);

			if (count + 1 == InstanceManager.getQuorum()) {
				System.out.printf("Server %d recieved valid prepare quorum of consensus %d %n", this.getID(), message.getInstance());
				prepared_round = message.getRound();
				prepared_value = message.getValue();
				BFTMessage toBroadcast = new BFTMessage(BFTMessage.Type.COMMIT, message.getInstance(), message.getRound(), message.getValue());
				ibftBroadcast.broadcast(toBroadcast);
			}
		}
	}

	private void handleCommit(LinkMessage commit) {
		BFTMessage message = (BFTMessage) commit.getMessage();

		synchronized(commitCount) {
			commitCount.putIfAbsent(message, 0);

			int count = commitCount.get(message);

			commitCount.put(message, count + 1);

			if (count + 1 == InstanceManager.getQuorum()) {
				System.out.printf("Server %d recieved valid commit quorum of consensus %d with value '%s' %n", this.getID(), message.getInstance(), message.getValue());
				decide(message);
			}
		}
	}

	private void handleRoundChange(LinkMessage round_change) {
		// not needed for now
	}

	private void decide(BFTMessage message) {
		// maybe change to next instance here?
		blockchainState.append(message.getValue());
		HDLProcess client = pendingRequests.remove(message.getValue());
		LinkMessage response = new LinkMessage(new ClientMessage(ClientMessage.Type.RESPONSE, ClientMessage.Status.OK), this, client);
		channel.send(response);
	}

	public void kill() {
		this.running = false;
		try {
			ClientMessage dummy = new ClientMessage(ClientMessage.Type.REQUEST, "KYS (in-game)");
			LinkMessage killMessage = new LinkMessage(dummy, this, this, true);
			System.out.printf("kilelele for %d\n", this.getID());
			channel.send(killMessage);
		}
		catch (IllegalStateException ile) {
			System.err.printf("Tried to kill server %d but channel was already closed or receiver terminated\n", this.getID());
		}
	}
}
