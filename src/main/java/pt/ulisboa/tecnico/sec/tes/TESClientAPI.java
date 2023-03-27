package pt.ulisboa.tecnico.sec.tes;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
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

public class TESClientAPI extends HDLProcess {

    private AuthenticatedPerfectLink channel;

    public TESClientAPI(int id) throws UnknownHostException {
        super(id);
        this.channel = new AuthenticatedPerfectLink(this);
    }

    public ClientResponseMessage append(String string) throws IllegalStateException, InterruptedException {
        // protecting against client multithread
        synchronized(this) {

            List<Integer> sendersId = new ArrayList<>();
            Map<SimpleImmutableEntry<ClientResponseMessage.Status, Integer>, Integer> responsesCount = new HashMap<>();

            ClientRequestMessage request = new ClientRequestMessage(string);
            BestEffortBroadcast broadcastChannel = new BestEffortBroadcast(channel, InstanceManager.getAllParticipants());
            broadcastChannel.broadcast(request);

            // Waiting until we get MAX responses allowed
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
