package pt.tecnico.ibft;

import java.net.UnknownHostException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import pt.tecnico.broadcasts.BestEffortBroadcast;
import pt.tecnico.instances.InstanceManager;
import pt.tecnico.links.AuthenticatedPerfectLink;
import pt.tecnico.messages.ClientRequestMessage;
import pt.tecnico.messages.ClientResponseMessage;
import pt.tecnico.messages.Message;

public class IBFTClientAPI extends HDLProcess {

    private AuthenticatedPerfectLink channel;

    public IBFTClientAPI(int id) throws UnknownHostException {
        super(id);
        this.channel = new AuthenticatedPerfectLink(this);
    }

    public ClientResponseMessage.Status append(String string) throws IllegalStateException, InterruptedException {
        // protecting against client multithread
        synchronized(this) {

            Map<ClientResponseMessage.Status, Integer> responses = new EnumMap<>(ClientResponseMessage.Status.class);

            int numberResponses = 0;

            ClientRequestMessage request = new ClientRequestMessage(string);
            BestEffortBroadcast broadcastChannel = new BestEffortBroadcast(channel, InstanceManager.getServerProcesses());
            broadcastChannel.broadcast(request);

            while (numberResponses < InstanceManager.getTotalNumberServers()) {
                Message response = broadcastChannel.deliver();

                if (!response.getMessageType().equals(Message.MessageType.CLIENT_RESPONSE))
                    continue;

                ClientResponseMessage message = (ClientResponseMessage) response;

                responses.putIfAbsent(message.getStatus(), 0);

                int amount = responses.get(message.getStatus());

                if (amount + 1 == InstanceManager.getNumberOfByzantines() || (InstanceManager.getNumberOfByzantines() == 0))
                    return message.getStatus();

                responses.put(message.getStatus(), amount + 1);

                numberResponses++;
            }

            return ClientResponseMessage.Status.REJECTED;
        }
    }

    public void shutdown() {
        this.selfTerminate();
        channel.close();
    }
}
