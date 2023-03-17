package pt.ulisboa.tecnico.sec.broadcasts;

import java.net.SocketTimeoutException;
import java.util.List;

import pt.ulisboa.tecnico.sec.ibft.HDLProcess;
import pt.ulisboa.tecnico.sec.links.AuthenticatedPerfectLink;
import pt.ulisboa.tecnico.sec.messages.LinkMessage;
import pt.ulisboa.tecnico.sec.messages.Message;

// Best effort broadcast using Authenticated Perfect links
public class BestEffortBroadcast {

    private AuthenticatedPerfectLink alInstance;
    private List<HDLProcess> systemServers;

    public BestEffortBroadcast(AuthenticatedPerfectLink alInstance, List<HDLProcess> systemServers) {
        this.alInstance = alInstance;
        this.systemServers = systemServers;
    }

    public void broadcast(Message message) throws IllegalStateException, InterruptedException {
        System.err.printf("[%s] BEB: Broadcasting message '%s'...%n", alInstance.getChannelOwner(), message);
        for (HDLProcess pj: systemServers) {
            LinkMessage linkMessage = new LinkMessage(message, alInstance.getChannelOwner(), pj);
            alInstance.send(linkMessage);
        }
    }

    public LinkMessage deliver() throws IllegalStateException, InterruptedException, SocketTimeoutException {
        LinkMessage linkMessage = alInstance.deliver();
        System.err.printf("[%s] BEB: Received link message %s%n", alInstance.getChannelOwner(), linkMessage);
        return linkMessage;
    }

    public void close() {
        alInstance.close();
    }
}
