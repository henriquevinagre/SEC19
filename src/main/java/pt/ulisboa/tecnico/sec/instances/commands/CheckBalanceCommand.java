package pt.ulisboa.tecnico.sec.instances.commands;

import java.security.PublicKey;
import java.security.PrivateKey;

import pt.ulisboa.tecnico.sec.instances.Client;
import pt.ulisboa.tecnico.sec.messages.ClientResponseMessage;
import pt.ulisboa.tecnico.sec.tes.TESClientAPI;

// Class for the Check Balance Command
public class CheckBalanceCommand extends Command { // cmd: T B 1 1

    private Client owner;

    public CheckBalanceCommand(Client sender, Client owner) {
        super(sender);
        this.owner = owner;
    }

    public ClientResponseMessage applyCommand() throws InterruptedException {
        TESClientAPI api = (TESClientAPI) getSender().getHDLInstance();
        PublicKey senderKey = api.getPublicKey();
        PrivateKey senderAuthKey = api.getPrivateKey();
        PublicKey ownerKey = owner.getHDLInstance().getPublicKey();

        return api.checkBalance(senderKey, ownerKey, senderAuthKey);
    }

    @Override
    public String toString() {
        return String.format("[ Check Balance of %s, sent by %s ]", owner.getID(), getSender().getID());
    }
}