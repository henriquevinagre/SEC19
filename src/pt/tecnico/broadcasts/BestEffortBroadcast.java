package pt.tecnico.broadcasts;

import java.io.IOException;
import java.util.List;

import pt.tecnico.instances.HDLProcess;
import pt.tecnico.links.AuthenticatedPerfectLink;
import pt.tecnico.messages.LinkMessage;
import pt.tecnico.messages.Message;

// Best effort broadcast using Authenticated Perfect links
public class BestEffortBroadcast {

    private AuthenticatedPerfectLink alInstance;
    private List<HDLProcess> otherProcesses;

    public BestEffortBroadcast(AuthenticatedPerfectLink alInstance, List<HDLProcess> otherProcesses) {
        this.alInstance = alInstance;
        this.otherProcesses = otherProcesses;
    }

    public void broadcast(Message message) throws IOException {
        System.err.println("BEB: Broadcasting message " + message.toString() + "...");
        for (HDLProcess pj: otherProcesses) {
            LinkMessage linkMessage = new LinkMessage(message, alInstance.getChannelOwner(), pj);
            alInstance.alp2pSend(linkMessage);
        }
    }

    public LinkMessage deliver() throws IOException {
        return alInstance.alp2pDeliver();
    }

    public void close() {
        alInstance.close();
    }
}
