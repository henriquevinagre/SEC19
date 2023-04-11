package pt.ulisboa.tecnico.sec.instances;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import pt.ulisboa.tecnico.sec.broadcasts.BestEffortBroadcast;
import pt.ulisboa.tecnico.sec.ibft.HDLProcess;
import pt.ulisboa.tecnico.sec.ibft.IBFTValueIT;
import pt.ulisboa.tecnico.sec.messages.BFTMessage;
import pt.ulisboa.tecnico.sec.messages.LinkMessage;

@SuppressWarnings("unchecked")
public class Consensus<T extends IBFTValueIT> {
	private HDLProcess process;

	private BestEffortBroadcast ibftBroadcast;

	// IBFT related variables
	private int instance = 0;
	private Object instanceLock = new Object();
	private int round = 0;
	private Map<BFTMessage<T>, Set<Integer>> prepareCount;
	private Map<BFTMessage<T>, Set<Integer>> commitCount;


	public Consensus(HDLProcess process, BestEffortBroadcast ibftBroadcast) {
		this.process = process;
		this.ibftBroadcast = ibftBroadcast;
		this.prepareCount = new HashMap<>();
		this.commitCount = new HashMap<>();
	}

	public Integer getInstance() {
		return this.instance;
	}

	public Integer incrementInstance() {
		return ++this.instance;
	}

	public Integer getRound() {
		return this.round;
	}

	// Start IBFT protocol if this process is the leader
	public void startConsensus(T value) throws InterruptedException {
		int currentInstance = 0;
		synchronized (instanceLock) {
			currentInstance = instance++;
		}
		if (this.process.equals(InstanceManager.getLeader(currentInstance, round))) {
			System.out.printf("[L] Server %d starting instance %d of consensus %n", process.getID(), currentInstance);
			// Creates PRE_PREPARE message
			BFTMessage<T> pre_prepare = new BFTMessage<>(BFTMessage.Type.PRE_PREPARE, currentInstance, round, value);
			pre_prepare.signMessage(process.getPrivateKey());
			// Broadcasts PRE_PREPARE
			ibftBroadcast.broadcast(pre_prepare); // FIXME: HMAC & SIGNATURES ???
		}
	}

	public void handlePrePrepare(LinkMessage pre_prepare) throws InterruptedException {
		int currentInstance;
		synchronized (instanceLock) {
			currentInstance = instance;
		}

		// Authenticates sender of the PRE_PREPARE message as the Leader (JUSTIFY_PRE_PREPARE)
		if (!InstanceManager.getLeader(currentInstance, round).equals(pre_prepare.getSender()) ||
			!pre_prepare.getMessage().hasValidSignature(pre_prepare.getSender().getPublicKey()))
			return;

		BFTMessage<T> message = (BFTMessage<T>) pre_prepare.getMessage();

		System.err.printf("%sServer %d received valid PRE_PREPARE from %d of consensus %d%n",
			InstanceManager.getLeader(currentInstance, round).equals(process)? "[L] ": "", process.getID(), pre_prepare.getSender().getID(), message.getInstance());

		// Creates PREPARE message
		BFTMessage<T> prepare = new BFTMessage<>(BFTMessage.Type.PREPARE, message.getInstance(), message.getRound(), message.getValue());

		// Broadcasts PREPARE
		ibftBroadcast.broadcast(prepare);
	}

	public void handlePrepare(LinkMessage prepare) throws InterruptedException {
		BFTMessage<T> message = (BFTMessage<T>) prepare.getMessage();

		System.err.printf("%sServer %d received valid PREPARE from %d of consensus %d %n",
			InstanceManager.getLeader(message.getInstance(), round).equals(process)? "[L] ": "", process.getID(), prepare.getSender().getID(), message.getInstance());

		int count = 0;
		synchronized (prepareCount) {
			prepareCount.putIfAbsent(message, new HashSet<>());
			prepareCount.get(message).add(prepare.getSender().getID());
			count = prepareCount.get(message).size();
		}

		// Reaching a quorum of PREPARE messages (given by different servers)
		if (count == InstanceManager.getQuorum()) {
			System.out.printf("%sServer %d received valid PREPARE quorum of consensus %d with value %s %n",
				InstanceManager.getLeader(message.getInstance(), round).equals(process)? "[L] ": "", process.getID(), message.getInstance(), message.getValue());


			// Creates COMMIT message
			BFTMessage<T> commit = new BFTMessage<>(BFTMessage.Type.COMMIT, message.getInstance(), message.getRound(), message.getValue());

			// Broadcasts COMMIT
			ibftBroadcast.broadcast(commit);
		}
	}

	public BFTMessage<T> handleCommit(LinkMessage commit) throws InterruptedException {
		BFTMessage<T> message = (BFTMessage<T>) commit.getMessage();

		System.err.printf("%sServer %d received valid COMMIT from %d of consensus %d %n",
			InstanceManager.getLeader(message.getInstance(), round).equals(process)? "[L] ": "", process.getID(), commit.getSender().getID(), message.getInstance());

		int count = 0;
		synchronized (commitCount) {
			commitCount.putIfAbsent(message, new HashSet<>());
			commitCount.get(message).add(commit.getSender().getID());
			count = commitCount.get(message).size();
		}

		// Reaching a quorum of COMMIT messages (given by different servers)
		if (count == InstanceManager.getQuorum()) {
			System.out.printf("%sServer %d received valid COMMIT quorum of consensus %d with value %s %n",
				InstanceManager.getLeader(message.getInstance(), round).equals(process)? "[L] ": "", process.getID(), message.getInstance(), message.getValue());

			// Performs DECIDE
			return message;
		}

		return null;
	}
}
