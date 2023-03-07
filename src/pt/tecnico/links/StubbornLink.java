package pt.tecnico.links;

import java.io.IOException;
import java.net.InetAddress;

import pt.tecnico.messages.ACKMessage;
import pt.tecnico.messages.LinkMessage;
import pt.tecnico.messages.Message;

// Stubborn point to point link using Fair loss links
public class StubbornLink {
    
    private FairLossLink _flInstance;

    public StubbornLink(InetAddress specAddress, int specPort) {
        _flInstance = new FairLossLink(specAddress, specPort);
    }

    public StubbornLink(int specPort) {
        _flInstance = new FairLossLink(specPort);
    }

    public StubbornLink() {
        _flInstance = new FairLossLink();
    }

    public void sp2pSend(LinkMessage message) throws IOException {
        int count = 0;
        while (true) {
            count++;
            _flInstance.flp2pSend(message);

            // TODO timeout

            // Waiting for ACK if timeout trigger
            LinkMessage linkMessage = _flInstance.flp2pDeliver();

            // Verifying ACK
            if (!(linkMessage.getMessage().getMessageType().equals(Message.MessageType.ACK))) 
                continue; // Ignoring ACK. Continue sending messages
            
            ACKMessage ack = (ACKMessage) linkMessage.getMessage();
            if (message.getId() == ack.getReferId()) {
                System.err.println("SL: ACK verified after " + count + " attempts!");
                break;
            }

            // Ignoring ACK. Continue sending messages
        }
    }

    public LinkMessage sp2pDeliver() {
        LinkMessage message = _flInstance.flp2pDeliver();

        // Sending ACK to sender as a stop point

        // Creating ACK for the message
        ACKMessage ack = new ACKMessage(message.getId());
        LinkMessage ackMessage = new LinkMessage(ack, message.getEndHostAddress(), message.getEndHostPort());

        // Using fair loss link to send the ACK
        _flInstance.flp2pSend(ackMessage);

        return message;
    }

    public void close() {
        _flInstance.close();
    }

}
