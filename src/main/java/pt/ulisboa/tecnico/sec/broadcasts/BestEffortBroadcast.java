package pt.ulisboa.tecnico.sec.broadcasts;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import pt.ulisboa.tecnico.sec.ibft.ByzantineHandler;
import pt.ulisboa.tecnico.sec.ibft.HDLProcess;
import pt.ulisboa.tecnico.sec.ibft.ByzantineHandler.ByzantineBehaviour;
import pt.ulisboa.tecnico.sec.links.Channel;
import pt.ulisboa.tecnico.sec.messages.LinkMessage;
import pt.ulisboa.tecnico.sec.messages.Message;

// Best effort broadcast using Authenticated Perfect links
public class BestEffortBroadcast {

    private Channel channel;
    private List<HDLProcess> systemServers;

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

    public void broadcast(Message message) throws IllegalStateException, InterruptedException {
        System.err.printf("[%s] BEB: Broadcasting message '%s'...%n", channel.getChannelOwner(), message);
        
        List<HDLProcess> broadcastTo = new ArrayList<>(systemServers);

        // [B3] Incomplete broadcast behaviour
        // if (ByzantineHandler.withBehaviourActive(this.channel.getChannelOwner(), ByzantineBehaviour.INCOMPLETE_BROADCAST)) {
        //     System.err.printf("[B3] Server %d not completed broadcast %n", this.channel.getChannelOwner().getID());
            
        //     // Choose randomly the processes to broadcast
        //     Random random = new Random();
        //     int numBroadcasts = random.nextInt(0, systemServers.size() - 1);
        //     while (broadcastTo.size() > numBroadcasts) {
        //         int index = random.nextInt(0, broadcastTo.size());
        //         broadcastTo.remove(index);
        //     }
        // }                    Not working 100%
        
        for (HDLProcess pj: broadcastTo) {
            LinkMessage linkMessage = new LinkMessage(message, channel.getChannelOwner(), pj);
            channel.send(linkMessage);
        }
    }

    public LinkMessage deliver() throws IllegalStateException, InterruptedException, SocketTimeoutException {
        LinkMessage linkMessage = channel.deliver();
        System.err.printf("[%s] BEB: Received link message %s%n", channel.getChannelOwner(), linkMessage);
        return linkMessage;
    }

    public void close() {
        channel.close();
    }
}
