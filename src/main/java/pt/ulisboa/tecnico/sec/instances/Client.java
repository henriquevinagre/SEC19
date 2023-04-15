package pt.ulisboa.tecnico.sec.instances;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.sec.ibft.HDLProcess;
import pt.ulisboa.tecnico.sec.instances.commands.Command;
import pt.ulisboa.tecnico.sec.messages.ClientResponseMessage;
import pt.ulisboa.tecnico.sec.tes.TESClientAPI;

public class Client {

	private List<Command> commands;
	private int id;
	private TESClientAPI api;

	// for test purposes
	private List<ClientResponseMessage> responses;

	public Client(int id) throws UnknownHostException {
		this.id = id;
		api = new TESClientAPI(id);
		commands = new ArrayList<>();
		responses = new ArrayList<>();
	}

	public Client(int id, List<Command> commands) throws UnknownHostException {
		this(id);
		this.commands = commands;
	}

	public int getID() {
		return id;
	}

	public void addCommand(Command command) {
		this.commands.add(command);
	}

    public List<Command> getCommands() {
        return commands;
    }

	public HDLProcess getHDLInstance() {
		return api;
	}

	public List<ClientResponseMessage> getResponse() {
		return responses;
	}

	public void execute() {

		for (Command command: commands) {
			ClientResponseMessage response = null;

			try {
				System.out.printf("Client %d: Executing command '%s'%n", this.id, command);
				response = command.applyCommand();
			} catch (IllegalStateException | InterruptedException e) {
				e.printStackTrace(System.out);
				System.err.println("[ERROR] Client %d: Something was wrong executing " + command);
				continue;
			}

			assert(response != null);

			System.err.printf("Client %d: Request for transaction '%s' completed with status '%s' on timestamp '%s'%n", this.id, command, response.getStatus(), response.getTimestamp());
			responses.add(response);
		}

		api.shutdown();
		System.out.printf("Client %d: API closed, responses were: %s%n", this.id, getResponsesString());
	}

	private String getResponsesString() {
		return "[ " + String.join(", ", responses.stream().map((r) -> "< " + r.toString() + " > ").toArray(CharSequence[]::new)) + " ] ";
	}
}
