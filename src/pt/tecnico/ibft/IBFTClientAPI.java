package pt.tecnico.ibft;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.EnumMap;
import java.util.Map;

import pt.tecnico.broadcasts.BestEffortBroadcast;
import pt.tecnico.instances.HDLProcess;
import pt.tecnico.instances.InstanceManager;
import pt.tecnico.links.AuthenticatedPerfectLink;
import pt.tecnico.messages.ClientMessage;
import pt.tecnico.messages.Message;

public class IBFTClientAPI {
    private HDLProcess clientProcess;
    private AuthenticatedPerfectLink channel;

    public IBFTClientAPI(int id) throws UnknownHostException {
        this.clientProcess = new HDLProcess(id);
        this.channel = new AuthenticatedPerfectLink(clientProcess);
    }

    public ClientMessage.Status append(String string) throws IOException, IllegalStateException, InterruptedException {
        Map<ClientMessage.Status, Integer> responses = new EnumMap<>(ClientMessage.Status.class);

        for (ClientMessage.Status status : ClientMessage.Status.values()) {
            responses.put(status, 0);
        }

        int numberResponses = 0;

        ClientMessage request = new ClientMessage(ClientMessage.Type.REQUEST, string);
        BestEffortBroadcast broadcastChannel = new BestEffortBroadcast(channel, InstanceManager.getServerProcesses());
		broadcastChannel.broadcast(request);

        while(numberResponses < InstanceManager.getTotalNumberServers()) {
            Message response = broadcastChannel.deliver().getMessage();

            if(!response.getMessageType().equals(Message.MessageType.CLIENT))
                continue;

            ClientMessage message = (ClientMessage) response;

            int amount = responses.get(message.getStatus());

            if (amount + 1 == InstanceManager.getNumberOfByzantines() || (InstanceManager.getNumberOfByzantines() == 0))
                return message.getStatus();

            responses.put(message.getStatus(), amount + 1);

            numberResponses++;
        }

        return ClientMessage.Status.REJECTED;
    }

    public void shutdown() {
        channel.close();
    }
}
