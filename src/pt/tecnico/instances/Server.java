package pt.tecnico.instances;

import java.net.UnknownHostException;

import pt.tecnico.ibft.HDLProcess;
import pt.tecnico.links.AuthenticatedPerfectLink;
import pt.tecnico.messages.ClientMessage;
import pt.tecnico.messages.LinkMessage;


public class Server extends HDLProcess {
	private boolean running = false;
	private AuthenticatedPerfectLink channel;

	public Server(int id, int port) throws UnknownHostException {
		super(id, port);
		channel = new AuthenticatedPerfectLink(this);
	}

	public void execute() {
		// Create channel
		// BestEffortBroadcast bebInstance = new BestEffortBroadcast(channel, List.of());
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

				// Get send process (info)
				HDLProcess clientProcess = requestMessage.getSender();

				// Send response to the client
				ClientMessage response = new ClientMessage(ClientMessage.Type.RESPONSE, ClientMessage.Status.OK);
				channel.send(new LinkMessage(response, this, clientProcess));
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
