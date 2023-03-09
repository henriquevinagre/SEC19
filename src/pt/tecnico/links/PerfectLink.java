package pt.tecnico.links;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import pt.tecnico.messages.LinkMessage;

// Perfect point to point link using Stubborn links
public class PerfectLink {
    
    private StubbornLink _slInstance;
    private List<LinkMessage> _delivered;

    public PerfectLink(InetAddress specAddress, int specPort) {
        _slInstance = new StubbornLink(specAddress, specPort);
        _delivered = new ArrayList<LinkMessage>();
    }

    public PerfectLink(int specPort) {
        _slInstance = new StubbornLink(specPort);
        _delivered = new ArrayList<LinkMessage>();
    }

    public PerfectLink() {
        _slInstance = new StubbornLink();
        _delivered = new ArrayList<LinkMessage>();
    }

    public void pp2pSend(LinkMessage message) throws IOException {
        System.err.println("PL: Sending message with id: " + message.getId() + "...");
        _slInstance.sp2pSend(message);
    }

    public LinkMessage pp2pDeliver() {
        LinkMessage message;

        // Wait for a message that was not delivered yet
        do {
            message = _slInstance.sp2pDeliver();
            System.err.println("PL: Received message with id: " + message.getId()); // by now the id is from a static counter
        } while (_delivered.contains(message));

        assert(message != null);

        System.err.println("PL: Message already not delivered! Delivering message...");

        _delivered.add(message);
        return message;
    }

    public void close() {
        _slInstance.close();
    }

}
