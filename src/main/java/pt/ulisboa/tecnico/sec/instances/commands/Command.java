package pt.ulisboa.tecnico.sec.instances.commands;

import pt.ulisboa.tecnico.sec.instances.Client;
import pt.ulisboa.tecnico.sec.messages.ClientResponseMessage;

// Class for Client commands
public abstract class Command {
    private Client sender;

    public Command(Client sender) {
        this.sender = sender;
    }

    public Client getSender() { return this.sender; }

    public abstract ClientResponseMessage applyCommand() throws InterruptedException;

    @Override
    public abstract String toString();
}