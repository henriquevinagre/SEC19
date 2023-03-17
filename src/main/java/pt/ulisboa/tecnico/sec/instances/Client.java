package pt.ulisboa.tecnico.sec.instances;

import java.net.UnknownHostException;

import pt.ulisboa.tecnico.sec.ibft.HDLProcess;
import pt.ulisboa.tecnico.sec.ibft.IBFTClientAPI;
import pt.ulisboa.tecnico.sec.messages.ClientResponseMessage;

public class Client {
	private String message;
	private int id;
	private IBFTClientAPI api;


	public Client(int id, String message) throws UnknownHostException {
		this.message = message;
		this.id = id;
		api = new IBFTClientAPI(id);
	}

	public String getMessage() {
		return message;
	}

	public HDLProcess getHDLInstance() {
		return api;
	}

	public void execute() {

		ClientResponseMessage response = null;
		try {
			System.out.printf("Client %d: Sending request to append the message '%s' on blockchain...%n", this.id, this.message);

			response = api.append(this.message);
			System.out.printf("Client %d: Request for message '" + this.message + "' completed with status %s %s%n", this.id, response.getStatus(),
				((response.getTimestamp() != null)? "at block " + response.getTimestamp() : ""));
		} catch (IllegalStateException | InterruptedException e) {
			System.out.printf("Client %d: Request did not perform well. Try again!", this.id);
		} finally {
			api.shutdown();
			System.out.printf("Client %d: API closed%n", this.id);
		}
	}
}
