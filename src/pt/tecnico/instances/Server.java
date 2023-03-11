package pt.tecnico.instances;

import java.io.*;
import java.net.UnknownHostException;
import java.util.List;

import pt.tecnico.broadcasts.BestEffortBroadcast;
import pt.tecnico.crypto.KeyHandler;
import pt.tecnico.links.AuthenticatedPerfectLink;
import pt.tecnico.messages.ClientMessage;
import pt.tecnico.messages.LinkMessage;
import pt.tecnico.messages.Message;


public class Server {
	private int port;
	private int id;
	private boolean running = false;
	private HDLProcess serverProcess;

	public Server(int id, int port) throws UnknownHostException {
		this.port = port;
		serverProcess = new HDLProcess(port);
		KeyHandler.generateKey(id);
	}

	public HDLProcess getHDLInstance() {
		return this.serverProcess;
	}

	public void execute() throws IOException {
		// Create channel
		AuthenticatedPerfectLink channel = new AuthenticatedPerfectLink(serverProcess);
		BestEffortBroadcast bebInstance = new BestEffortBroadcast(channel, List.of());
		System.out.printf("Server will receive messages on port %d %n", port);

		this.running = true;
		// Wait for client packets
		while (running) {
			// Receive packet
			System.out.println("Wait for some request from a client...");
			LinkMessage requestMessage = channel.alp2pDeliver();

			// Get send process (info)
			HDLProcess clientProcess = requestMessage.getEndHost();

			// Convert request to Message
			ClientMessage clientMessage = (ClientMessage) requestMessage.getMessage();

			if(requestMessage.getMessage().getMessageType().equals(Message.MessageType.BFT)) {
				// process algorithm
			} else if(requestMessage.getMessage().getMessageType().equals(Message.MessageType.CLIENT)) {
				// process consensus and respond
			}

			// ### IBFT algorithm ###
			bebInstance.broadcast(clientMessage);

			// Create response message
			ClientMessage response = new ClientMessage(ClientMessage.Type.RESPONSE, ClientMessage.Status.OK);
			System.out.println("Response message: " + response.getStatus().toString());

			// Send response
			LinkMessage responseMessage = new LinkMessage(response, clientProcess);
			channel.alp2pSend(responseMessage);
		}

		channel.close();
	}

	public void kill() {
		this.running = false;
	}
}
