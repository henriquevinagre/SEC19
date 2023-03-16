package pt.tecnico.ibft;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;

import pt.tecnico.broadcasts.BestEffortBroadcast;
import pt.tecnico.instances.InstanceManager;
import pt.tecnico.links.AuthenticatedPerfectLink;
import pt.tecnico.messages.ClientRequestMessage;
import pt.tecnico.messages.ClientResponseMessage;
import pt.tecnico.messages.LinkMessage;
import pt.tecnico.messages.Message;

public class IBFTClientAPI extends HDLProcess {

    private AuthenticatedPerfectLink channel;

    public IBFTClientAPI(int id) throws UnknownHostException {
        super(id);
        this.channel = new AuthenticatedPerfectLink(this);
    }

    public ClientResponseMessage append(String string) throws IllegalStateException, InterruptedException {
        // protecting against client multithread
        synchronized(this) {

            List<LinkMessage> responses = new ArrayList<>();
            List<Integer> sendersId = new ArrayList<>();
            Map<SimpleImmutableEntry<ClientResponseMessage.Status, Integer>, Integer> responsesCount = new HashMap<>();

            ClientRequestMessage request = new ClientRequestMessage(string);
            BestEffortBroadcast broadcastChannel = new BestEffortBroadcast(channel, InstanceManager.getServerProcesses());
            broadcastChannel.broadcast(request);

            // Waiting until we get MAX responses allowed
            while (responses.size() < InstanceManager.getTotalNumberServers()) {
                LinkMessage response = broadcastChannel.deliver();

                if (!response.getMessage().getMessageType().equals(Message.MessageType.CLIENT_RESPONSE) ||
                    sendersId.contains(response.getSender().getID()))
                    continue; // Ignoring response

                sendersId.add(response.getSender().getID());

                ClientResponseMessage message = (ClientResponseMessage) response.getMessage();

                SimpleImmutableEntry entry = new SimpleImmutableEntry<>(message.getStatus(), message.getTimestamp());
                responsesCount.putIfAbsent(entry, 0);
                
                int count = responsesCount.get(entry) + 1;

                responsesCount.put(entry, count);
                
                System.out.printf("API CLIENT %d received %s (responses number %d)%n", this._id, response, responses.size());

                if (count == InstanceManager.getNumberOfByzantines() + 1)
                    return message;
            }

            return new ClientResponseMessage(ClientResponseMessage.Status.REJECTED, null);
        }
    }

    public void shutdown() {
        this.selfTerminate();
        channel.close();
    }
}
