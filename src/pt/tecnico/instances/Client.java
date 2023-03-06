package pt.tecnico.instances;

import java.io.*;
import java.net.*;
import java.security.*;

import pt.tecnico.crypto.KeyHandler;
import pt.tecnico.messages.ClientMessage;
import pt.tecnico.messages.Message;


public class Client {

	/** Buffer size for receiving a UDP packet. */
	private static final int BUFFER_SIZE = 65_507;

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

		// Create socket
		DatagramSocket socket = new DatagramSocket();

        // Create request message
		ClientMessage request = new ClientMessage(ClientMessage.Type.REQUEST, "Add this string pls");
		request.signMessage(clientPrivKey);

		// Send request
		byte[] clientData = request.toByteArray();
		DatagramPacket clientPacket = new DatagramPacket(clientData, clientData.length, serverAddress, serverPort);
		socket.send(clientPacket);
		System.out.printf("Request packet sent to %s:%d!%n", serverAddress, serverPort);

		// Receive response
		byte[] serverData = new byte[BUFFER_SIZE];
		DatagramPacket serverPacket = new DatagramPacket(serverData, serverData.length);
		System.out.println("Wait for response packet...");
		socket.receive(serverPacket);
		System.out.printf("Received packet from %s:%d!%n", serverPacket.getAddress(), serverPacket.getPort());
		System.out.printf("%d bytes %n", serverPacket.getLength());

		// Convert response to Message
		Message serverMessage = Message.fromByteArray(serverPacket.getData());

		if(!serverMessage.getMessageType().equals(Message.MessageType.CLIENT)) {
			socket.close();
			throw new IllegalStateException("Client should have not received message of type: " + serverMessage.getMessageType().toString());
		}

		ClientMessage message = (ClientMessage) serverMessage;
		System.out.println("Response with status: " + message.geStatus().toString() + ", and signature " + message.hasValidSignature(serverPubKey));

		// Close socket
		socket.close();
		System.out.println("Socket closed");
	}

}
