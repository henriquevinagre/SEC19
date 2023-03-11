package pt.tecnico.instances;

import java.io.*;
import java.net.UnknownHostException;
import java.util.List;

import pt.tecnico.broadcasts.BestEffortBroadcast;
import pt.tecnico.links.AuthenticatedPerfectLink;
import pt.tecnico.messages.ClientMessage;
import pt.tecnico.messages.LinkMessage;
import pt.tecnico.messages.Message;


public class Server {
	private int port;
	private boolean running = false;
	private HDLProcess serverProcess;
	private AuthenticatedPerfectLink channel;

	public Server(int id, int port) throws UnknownHostException {
		this.port = port;
		serverProcess = new HDLProcess(id, port);
		channel = new AuthenticatedPerfectLink(serverProcess);
	}

	public HDLProcess getHDLInstance() {
		return this.serverProcess;
	}

	public void execute() throws IOException {
		// Create channel
		BestEffortBroadcast bebInstance = new BestEffortBroadcast(channel, List.of());
		System.out.printf("Server will receive messages on port %d %n", port);

		this.running = true;
		// Wait for client packets
		while (running) {
			// Receive packet
			System.out.println("Server " + serverProcess.getID() + " Waiting for some request from a client...");
			LinkMessage requestMessage = channel.alp2pDeliver();

			// Get send process (info)
			HDLProcess clientProcess = requestMessage.getSender();

			ClientMessage response = new ClientMessage(ClientMessage.Type.RESPONSE, ClientMessage.Status.OK);
			channel.alp2pSend(new LinkMessage(response, serverProcess, clientProcess));
		}

		channel.close();
	}

	public void kill() {
		this.running = false;
		this.channel.close();
	}
}
