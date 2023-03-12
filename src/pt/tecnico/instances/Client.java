package pt.tecnico.instances;

import java.io.*;

import pt.tecnico.ibft.IBFTClientAPI;
import pt.tecnico.messages.ClientMessage;

public class Client {
	private String message;
	private int id;


	public Client(int id, String message) {
		this.message = message;
		this.id = id;
	}

	public void execute() throws IOException, IllegalStateException, InterruptedException {
		IBFTClientAPI api = new IBFTClientAPI(id);

		System.out.println("Client " + id + ": Sending request to append message: " + this.message);
		ClientMessage.Status responseStatus = api.append(this.message);

		System.out.println("Client " + id + ": Request completed with status: " + responseStatus);

		api.shutdown();
	}

}
