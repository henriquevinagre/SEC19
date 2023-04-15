package pt.ulisboa.tecnico.sec.instances.commands;

import java.security.PublicKey;
import java.security.PrivateKey;

import pt.ulisboa.tecnico.sec.instances.Client;
import pt.ulisboa.tecnico.sec.messages.ClientResponseMessage;
import pt.ulisboa.tecnico.sec.tes.TESClientAPI;
import pt.ulisboa.tecnico.sec.tes.transactions.CheckBalanceTransaction.ReadType;

// Class for the Check Balance Command
public class CheckBalanceCommand extends Command { // cmd: T B X Y [WS]

    private Client owner;
    private ReadType readType;

    public CheckBalanceCommand(Client sender, Client owner, ReadType readType) {
        super(sender);
        this.owner = owner;
        this.readType = readType;
    }

    public ClientResponseMessage applyCommand() throws InterruptedException {
        TESClientAPI api = (TESClientAPI) getSender().getHDLInstance();
        PublicKey senderKey = api.getPublicKey();
        PrivateKey senderAuthKey = api.getPrivateKey();
        PublicKey ownerKey = owner.getHDLInstance().getPublicKey();

        return api.checkBalance(senderKey, ownerKey, senderAuthKey, readType);
    }

    @Override
    public String toString() {
        return String.format("[ Check Balance of %s, sent by %s with mode %s]", owner.getID(), getSender().getID(), readType);
    }
}