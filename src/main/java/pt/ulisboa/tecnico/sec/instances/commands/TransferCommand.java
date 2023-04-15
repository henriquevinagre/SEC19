package pt.ulisboa.tecnico.sec.instances.commands;

import java.security.PublicKey;
import java.security.PrivateKey;

import pt.ulisboa.tecnico.sec.instances.Client;
import pt.ulisboa.tecnico.sec.messages.ClientResponseMessage;
import pt.ulisboa.tecnico.sec.tes.TESClientAPI;

// Class for the Create Account Command
public class TransferCommand extends Command { // cmd: T T sender receiver amount

    private Client receiver;
    private double amount;

    public TransferCommand(Client sender, Client receiver, double amount) {
        super(sender);
        this.receiver = receiver;
        this.amount = amount;
    }

    public ClientResponseMessage applyCommand() throws InterruptedException {
        TESClientAPI api = (TESClientAPI) getSender().getHDLInstance();
        PublicKey senderKey = api.getPublicKey();
        PrivateKey senderAuthKey = api.getPrivateKey();
        PublicKey receiverKey = receiver.getHDLInstance().getPublicKey();

        return api.transfer(senderKey, receiverKey, amount, senderAuthKey);
    }

    @Override
    public String toString() {
        return String.format("[ Transfer %.4f tucs from %s to %s ]", amount, getSender().getID(), receiver.getID());
    }
}