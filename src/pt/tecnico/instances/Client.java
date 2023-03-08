package pt.tecnico.instances;

import java.io.*;
import java.net.*;
import java.security.*;

import pt.tecnico.crypto.KeyHandler;
import pt.tecnico.links.PerfectLink;
import pt.tecnico.messages.ClientMessage;
import pt.tecnico.messages.LinkMessage;
import pt.tecnico.messages.Message;


public class Client {


	public static void main(String[] args) throws IOException {
		// Check arguments
		if (args.length < 2) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s host port%n", Client.class.getName());
			return;
		}
		final String serverHost = args[0];
		final InetAddress serverAddress = InetAddress.getByName(serverHost);
		final int serverPort = Integer.parseInt(args[1]);

		PrivateKey clientPrivKey = KeyHandler.getPrivateKey("keys/client.key");
		PublicKey serverPubKey = KeyHandler.getPublicKey("keys/server.pub.key");

		// Create channel
		PerfectLink channel = new PerfectLink();

        // Create request message
		ClientMessage request = new ClientMessage(ClientMessage.Type.REQUEST, "Add this string pls");
		request.signMessage(clientPrivKey);

		// Send request
		LinkMessage requestMessage = new LinkMessage(request, serverAddress, serverPort);
		channel.pp2pSend(requestMessage);


		// Receive response
		System.out.println("Wait for server response...");
		LinkMessage responseMessage = channel.pp2pDeliver();

		// Convert response to Message
		Message response = responseMessage.getMessage();

		if(!response.getMessageType().equals(Message.MessageType.CLIENT)) {
			channel.close();
			throw new IllegalStateException("Client should have not received message of type: " + response.getMessageType().toString());
		}

		ClientMessage message = (ClientMessage) response;
		System.out.println("Response with status: " + message.getStatus().toString() + ", and signature " + message.hasValidSignature(serverPubKey));

		// Close channel
		channel.close();
		System.out.println("Channel closed");
	}

}
