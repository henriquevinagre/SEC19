package pt.ulisboa.tecnico.sec.tes;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;

import pt.ulisboa.tecnico.sec.broadcasts.BestEffortBroadcast;
import pt.ulisboa.tecnico.sec.ibft.HDLProcess;
import pt.ulisboa.tecnico.sec.instances.InstanceManager;
import pt.ulisboa.tecnico.sec.links.AuthenticatedPerfectLink;
import pt.ulisboa.tecnico.sec.messages.ClientRequestMessage;
import pt.ulisboa.tecnico.sec.messages.ClientResponseMessage;
import pt.ulisboa.tecnico.sec.messages.LinkMessage;
import pt.ulisboa.tecnico.sec.messages.Message;
import pt.ulisboa.tecnico.sec.tes.transactions.CheckBalanceTransaction;
import pt.ulisboa.tecnico.sec.tes.transactions.CreateAccountTransaction;
import pt.ulisboa.tecnico.sec.tes.transactions.Transaction;
import pt.ulisboa.tecnico.sec.tes.transactions.TransferTransaction;
import pt.ulisboa.tecnico.sec.tes.transactions.Transaction.TESOperation;

public class TESClientAPI extends HDLProcess {

    private AuthenticatedPerfectLink channel;
    private int nonce;

    public TESClientAPI(int id) throws UnknownHostException {
        super(id);
        this.channel = new AuthenticatedPerfectLink(this);
        nonce = 0;
    }

    public ClientResponseMessage createAccount(PublicKey source, PrivateKey sourceAuthKey) throws IllegalStateException, InterruptedException {
        Transaction t = new CreateAccountTransaction(source);
        t.authenticateTransaction(nonce++, sourceAuthKey);
        return this.appendTransaction(t);
    }

    public ClientResponseMessage transfer(PublicKey source, PublicKey destination, double amount, PrivateKey sourceAuthKey) throws IllegalStateException, InterruptedException {
        Transaction t = new TransferTransaction(source, destination, amount);
        t.authenticateTransaction(nonce++, sourceAuthKey);
        return this.appendTransaction(t);
    }

    // FIXME: v
    public ClientResponseMessage checkBalance(PublicKey source, PublicKey owner, PrivateKey sourceAuthKey) throws IllegalStateException, InterruptedException {
        Transaction t = new CheckBalanceTransaction(source, owner);
        t.authenticateTransaction(nonce++, sourceAuthKey);  // FIXME: checkbalance transaction must not be in the blockchain's blocks
        return new ClientResponseMessage(ClientResponseMessage.Status.REJECTED, -1);
    }

    private ClientResponseMessage appendTransaction(Transaction transaction) throws IllegalStateException, InterruptedException {
        // Protecting against client multithread
        synchronized(this) {
            List<Integer> sendersId = new ArrayList<>();
            Map<SimpleImmutableEntry<ClientResponseMessage.Status, Integer>, Integer> responsesCount = new HashMap<>();

            ClientRequestMessage request = new ClientRequestMessage(transaction);
            BestEffortBroadcast broadcastChannel = new BestEffortBroadcast(channel, InstanceManager.getAllParticipants());
            broadcastChannel.broadcast(request);

            // Waiting until get f+1 responses
            while (true) {
                LinkMessage response = null;
                try {
                    response = broadcastChannel.deliver();
                } catch (SocketTimeoutException e) {
                    continue;
                }

                if (!response.getMessage().getMessageType().equals(Message.MessageType.CLIENT_RESPONSE) ||
                    sendersId.contains(response.getSender().getID()))
                    continue; // Ignoring response

                sendersId.add(response.getSender().getID());

                ClientResponseMessage message = (ClientResponseMessage) response.getMessage();

                SimpleImmutableEntry<ClientResponseMessage.Status, Integer> entry = new SimpleImmutableEntry<>(message.getStatus(), message.getTimestamp());
                responsesCount.putIfAbsent(entry, 0);

                int count = responsesCount.get(entry) + 1;

                responsesCount.put(entry, count);

                System.out.printf("API CLIENT %d received %s (responses number %d)%n", this._id, response, count);

                if (count == InstanceManager.getNumberOfByzantines() + 1)
                    return message;
            }
        }
    }

    public void shutdown() {
        this.selfTerminate();
        channel.close();
    }
}
