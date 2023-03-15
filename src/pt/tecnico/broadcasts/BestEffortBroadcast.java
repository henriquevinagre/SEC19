package pt.tecnico.broadcasts;

import java.util.List;

import pt.tecnico.ibft.HDLProcess;
import pt.tecnico.links.AuthenticatedPerfectLink;
import pt.tecnico.messages.LinkMessage;
import pt.tecnico.messages.Message;

// Best effort broadcast using Authenticated Perfect links
public class BestEffortBroadcast {

    private AuthenticatedPerfectLink alInstance;
    private List<HDLProcess> systemServers;

    public BestEffortBroadcast(AuthenticatedPerfectLink alInstance, List<HDLProcess> systemServers) {
        this.alInstance = alInstance;
        this.systemServers = systemServers;
    }

    public void broadcast(Message message) throws IllegalStateException {
        System.err.printf("[%s] BEB: Broadcasting message '%s'...%n", alInstance.getChannelOwner(), message);
        for (HDLProcess pj: systemServers) {
            LinkMessage linkMessage = new LinkMessage(message, alInstance.getChannelOwner(), pj);
            alInstance.send(linkMessage);
        }
    }

    public Message deliver() throws IllegalStateException, InterruptedException {
        LinkMessage linkMessage = alInstance.deliver();
        Message message = linkMessage.getMessage();
        System.err.printf("[%s] BEB: Received link message %s%n", alInstance.getChannelOwner(), linkMessage);
        return message;
    }

    public void close() {
        alInstance.close();
    }
}
