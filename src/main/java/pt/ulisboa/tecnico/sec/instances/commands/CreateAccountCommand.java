package pt.ulisboa.tecnico.sec.instances.commands;

import java.security.PublicKey;
import java.security.PrivateKey;

import pt.ulisboa.tecnico.sec.instances.Client;
import pt.ulisboa.tecnico.sec.messages.ClientResponseMessage;
import pt.ulisboa.tecnico.sec.tes.TESClientAPI;

// Class for the Create Account Command
public class CreateAccountCommand extends Command { // cmd: T C client

    public CreateAccountCommand(Client sender) {
        super(sender);
    }

    public ClientResponseMessage applyCommand() throws InterruptedException {
        TESClientAPI api = (TESClientAPI) getSender().getHDLInstance();
        PublicKey senderKey = api.getPublicKey();
        PrivateKey senderAuthKey = api.getPrivateKey();

        return api.createAccount(senderKey, senderAuthKey);
    }

    @Override
    public String toString() {
        return String.format("[ Create Account of %s ]", getSender().getID());
    }
}