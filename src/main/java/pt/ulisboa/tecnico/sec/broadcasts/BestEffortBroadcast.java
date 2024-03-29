package pt.ulisboa.tecnico.sec.broadcasts;

import java.net.SocketTimeoutException;

import java.util.List;

import pt.ulisboa.tecnico.sec.ibft.HDLProcess;
import pt.ulisboa.tecnico.sec.links.Channel;
import pt.ulisboa.tecnico.sec.messages.LinkMessage;
import pt.ulisboa.tecnico.sec.messages.Message;

// Best effort broadcast using Authenticated Perfect links
public class BestEffortBroadcast {

    private Channel channel;
    private List<HDLProcess> systemServers;
    private Object bcastLock = new Object();
    private Object deliverLock = new Object();

    public BestEffortBroadcast(Channel channel, List<HDLProcess> systemServers) {
        this.channel = channel;
        this.systemServers = systemServers;
    }

    public Channel getChannel() {
        return channel;
    }

    public List<HDLProcess> getInteractProcesses() {
        return systemServers;
    }

    public void setInteractProcesses(List<HDLProcess> processes) {
        this.systemServers = processes;
    }

    public void broadcast(Message message) throws IllegalStateException, InterruptedException {
        synchronized (bcastLock) {
            System.err.printf("[%s] BEB: Broadcasting message '%s'...%n", channel.getChannelOwner(), message);
            for (HDLProcess pj: systemServers) {
                System.err.printf("[%d] BEB: sending '%s' to %d...%n", channel.getChannelOwner().getID(), message, pj.getID());
                LinkMessage linkMessage = new LinkMessage(message, channel.getChannelOwner(), pj);
                try {
                    channel.send(linkMessage);
                } catch (InterruptedException | IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public LinkMessage deliver() throws IllegalStateException, InterruptedException, SocketTimeoutException {
        synchronized (deliverLock) {
            LinkMessage linkMessage = channel.deliver();
            System.err.printf("[%s] BEB: Received link message %s%n", channel.getChannelOwner(), linkMessage);
            return linkMessage;
        }
    }

    public void close() {
        channel.close();
    }
}
