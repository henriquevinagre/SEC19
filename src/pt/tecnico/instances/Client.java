package pt.tecnico.instances;

import java.net.UnknownHostException;

import pt.tecnico.ibft.HDLProcess;
import pt.tecnico.ibft.IBFTClientAPI;
import pt.tecnico.messages.ClientMessage;

public class Client {
	private String message;
	private int id;
	private IBFTClientAPI api;


	public Client(int id, String message) throws UnknownHostException {
		this.message = message;
		this.id = id;
		api = new IBFTClientAPI(id);
	}

	public HDLProcess getHDLInstance() {
		return api;
	}

	public void execute() {

		ClientMessage.Status responseStatus = null;
		try {
			System.out.printf("Client %d: Sending request to append the message '%s' on blockchain...%n", this.id, this.message);
			responseStatus = api.append(this.message);

			System.out.printf("Client %d: Request completed with status: %s%n", this.id, responseStatus);
		} catch (IllegalStateException | InterruptedException e) {
			System.out.printf("Client %d: Request did not perform well :(", this.id);
		} finally {
			api.shutdown();
			System.out.printf("Client %d: API closed%n", this.id);
		}
	}
}
