package pt.ulisboa.tecnico.sec.instances;

import java.net.UnknownHostException;
import java.security.KeyPair;

import pt.ulisboa.tecnico.sec.crypto.KeyHandler;
import pt.ulisboa.tecnico.sec.ibft.HDLProcess;
import pt.ulisboa.tecnico.sec.messages.ClientResponseMessage;
import pt.ulisboa.tecnico.sec.tes.TESClientAPI;

public class Client {
	private String message;
	private int id;
	private TESClientAPI api;

	private ClientResponseMessage response;

	public Client(int id, String message) throws UnknownHostException {
		this.message = message;
		this.id = id;
		api = new TESClientAPI(id);
	}

    public String getMessage() {
        return message;
    }

	public HDLProcess getHDLInstance() {
		return api;
	}

	public ClientResponseMessage getResponse() {
		return response;
	}

	public void execute() {

		KeyPair accountPair = KeyHandler.generateAccountKeyPair();

		try {
			System.out.printf("Client %d: Sending request to append the message '%s' on blockchain...%n", this.id, this.message);

			response = api.createAccount(accountPair.getPublic(), accountPair.getPrivate());
			System.out.printf("Client %d: Request for message '" + this.message + "' completed with status %s %s%n", this.id, response.getStatus(),
				((response.getTimestamp() != null)? "at block " + response.getTimestamp() : ""));
			// api.transfer(accountPair.getPublic(), InstanceManager.getLeader(0, 0).getPublicKey(), 99f, accountPair.getPrivate());
		} catch (IllegalStateException | InterruptedException e) {
			System.out.printf("Client %d: Request did not perform well. Try again!", this.id);
		} finally {
			api.shutdown();
			System.out.printf("Client %d: API closed%n", this.id);
		}
	}
}
