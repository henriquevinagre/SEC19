package pt.tecnico.instances;

import java.io.*;
import java.security.*;

import pt.tecnico.crypto.KeyHandler;
import pt.tecnico.links.AuthenticatedPerfectLink;
import pt.tecnico.messages.ClientMessage;
import pt.tecnico.messages.LinkMessage;
import pt.tecnico.messages.Message;


public class Client {
	private String message;
	private int id;


	public Client(int id, String message) {
		this.message = message;
		this.id = id;
		KeyHandler.generateKey(id);
	}

	public void execute() throws IOException {
		// Create client process
		HDLProcess clientProcess = new HDLProcess(id);

		// Create channel point instance
		AuthenticatedPerfectLink channel = new AuthenticatedPerfectLink(clientProcess);

        // Create request message
		ClientMessage request = new ClientMessage(ClientMessage.Type.REQUEST, this.message);

		// Send request via channel
		// TODO port hardcoded
		LinkMessage requestMessage = new LinkMessage(request, clientProcess);
		channel.alp2pSend(requestMessage);


		// Receive response
		System.out.println("Wait for server response...");
		LinkMessage responseMessage = channel.alp2pDeliver();

		// Convert response to Message
		Message response = responseMessage.getMessage();

		if(!response.getMessageType().equals(Message.MessageType.CLIENT)) {
			channel.close();
			throw new IllegalStateException("Client should have not received message of type: " + response.getMessageType().toString());
		}

		ClientMessage message = (ClientMessage) response;
		System.out.println("Response with status: " + message.getStatus().toString());

		// Close channel
		channel.close();
		System.out.println("Channel closed");
	}

}
