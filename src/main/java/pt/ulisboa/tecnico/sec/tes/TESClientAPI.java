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
import pt.ulisboa.tecnico.sec.messages.CheckBalanceResponseMessage;
import pt.ulisboa.tecnico.sec.messages.ClientRequestMessage;
import pt.ulisboa.tecnico.sec.messages.ClientResponseMessage;
import pt.ulisboa.tecnico.sec.messages.LinkMessage;
import pt.ulisboa.tecnico.sec.messages.Message;
import pt.ulisboa.tecnico.sec.tes.transactions.CheckBalanceTransaction;
import pt.ulisboa.tecnico.sec.tes.transactions.CreateAccountTransaction;
import pt.ulisboa.tecnico.sec.tes.transactions.Transaction;
import pt.ulisboa.tecnico.sec.tes.transactions.TransferTransaction;
import pt.ulisboa.tecnico.sec.tes.transactions.CheckBalanceTransaction.ReadType;

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

    // TODO: Gotta implement reads!
    public ClientResponseMessage checkBalance(PublicKey source, PublicKey owner, PrivateKey sourceAuthKey, ReadType read) throws IllegalStateException, InterruptedException {
        Transaction t = new CheckBalanceTransaction(source, owner, read);
        t.authenticateTransaction(nonce++, sourceAuthKey);

        if (read == ReadType.STRONGLY_CONSISTENT)
            return waitForNServerResponses(t, InstanceManager.getNumberOfByzantines() + 1);
            
        // Weakly consistent read
        CheckBalanceResponseMessage balanceResponse = (CheckBalanceResponseMessage) waitForNServerResponses(t, 1);
        for (SignedTESAccount acc : balanceResponse.signedTESAccount()) {
            if (!acc.validateState(acc.getSigner())) {
                return new ClientResponseMessage(ClientResponseMessage.Status.REJECTED, balanceResponse.getTimestamp(), balanceResponse.getNonce());
            }
        }
    
        return balanceResponse;
    }

    private ClientResponseMessage appendTransaction(Transaction transaction) throws IllegalStateException, InterruptedException {
        return waitForNServerResponses(transaction, InstanceManager.getNumberOfByzantines() + 1);
    }

    private ClientResponseMessage waitForNServerResponses(Transaction transaction, int nResponses) throws IllegalStateException, InterruptedException {
        // Protecting against client multithread
        synchronized (this) {
            List<Integer> sendersId = new ArrayList<>();
            Map<SimpleImmutableEntry<ClientResponseMessage.Status, Integer>, Integer> responsesCount = new HashMap<>();

            ClientRequestMessage request = new ClientRequestMessage(transaction);
            BestEffortBroadcast broadcastChannel = new BestEffortBroadcast(channel, InstanceManager.getAllParticipants());
            broadcastChannel.broadcast(request);

            // Waiting until we get f+1 responses
            while (true) {
                LinkMessage response = null;
                try {
                    response = broadcastChannel.deliver();
                } catch (SocketTimeoutException e) {
                    continue;
                }
                
                System.out.printf("API CLIENT %d processing message %s for transaction %s%n", this._id, response, transaction.toString());
                if (!response.getMessage().getMessageType().equals(Message.MessageType.CLIENT_RESPONSE) ||
                    sendersId.contains(response.getSender().getID()))
                    continue; // Ignoring response

                ClientResponseMessage message = (ClientResponseMessage) response.getMessage();

                // Ignore if it's not response for our transaction
                if (message.getNonce() != transaction.getNonce()) continue;

                sendersId.add(response.getSender().getID());

                SimpleImmutableEntry<ClientResponseMessage.Status, Integer> entry = new SimpleImmutableEntry<>(message.getStatus(), message.getTimestamp());
                responsesCount.putIfAbsent(entry, 0);

                int count = responsesCount.get(entry) + 1;

                responsesCount.put(entry, count);

                System.out.printf("API CLIENT %d received %s for transaction %s (responses number %d / %d)%n", this._id, response, transaction.toString(), count, nResponses);

                if (count == nResponses)
                    return message;
            }
        }
    }

    public void shutdown() {
        this.selfTerminate();
        channel.close();
    }
}
