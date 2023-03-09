package pt.tecnico.instances;

import java.io.*;
import java.security.PrivateKey;
import java.security.PublicKey;

import pt.tecnico.crypto.KeyHandler;
import pt.tecnico.links.AuthenticatedPerfectLink;
import pt.tecnico.messages.ClientMessage;
import pt.tecnico.messages.LinkMessage;


public class Server {

	public static void main(String[] args) throws IOException {
		// Check arguments
		if (args.length < 1) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s port%n", Server.class.getName());
			return;
		}
		final int port = Integer.parseInt(args[0]);

		// Create server process
		HDLProcess serverProcess = new HDLProcess("localhost", port);

		KeyHandler.generateKeys();
		PrivateKey serverPrivKey = KeyHandler.getPrivateKey("keys/server.key");
		PublicKey clientPubKey = KeyHandler.getPublicKey("keys/client.pub.key");

		// Create channel
		AuthenticatedPerfectLink channel = new AuthenticatedPerfectLink(serverProcess, serverPrivKey, clientPubKey);
		System.out.printf("Server will receive messages on port %d %n", port);

		// Wait for client packets
		while (true) {
			// Receive packet
			System.out.println("Wait for some request from a client...");
			LinkMessage requestMessage = channel.alp2pDeliver();

			// Get send process (info)
			HDLProcess clientProcess = requestMessage.getEndHost();

			// Convert request to Message
			ClientMessage clientMessage = (ClientMessage) requestMessage.getMessage();

			// TODO check message type shenanigans
			System.out.println("Received request: " + clientMessage.getValue() + "/ Signature " + clientMessage.hasValidSignature(clientPubKey));


			// ### IBFT algorithm ###


			// Create response message
			ClientMessage response = new ClientMessage(ClientMessage.Type.RESPONSE, ClientMessage.Status.OK);
			System.out.println("Response message: " + response.getStatus().toString());

			// Send response
			LinkMessage responseMessage = new LinkMessage(response, clientProcess);
			channel.alp2pSend(responseMessage);
		}
	}
}
