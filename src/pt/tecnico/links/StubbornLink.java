package pt.tecnico.links;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import pt.tecnico.messages.ACKMessage;
import pt.tecnico.messages.LinkMessage;
import pt.tecnico.messages.Message;

// Stubborn point to point link using Fair loss links
public class StubbornLink {

    private static final int POOL_SIZE = 4;
    private static final int TIMEOUT_MS = 10000;
    
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

    private boolean timeout(LinkMessage sendMessage) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<LinkMessage> future = executor.submit(new Callable<LinkMessage>() {
                public LinkMessage call() {
                    return _flInstance.flp2pDeliver();
                }
            }
        );
        LinkMessage receivedMessage = null;
        try {
            receivedMessage = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException time_e) {
            System.err.println("SL: Timeout!!! Retransmiting more...");
            return true;
        } finally {
            future.cancel(true);
            executor.shutdownNow();
        }

        assert(receivedMessage != null);

        // Verify ACK
        if (!(receivedMessage.getMessage().getMessageType().equals(Message.MessageType.ACK)))
            return true; // Ignoring ACK. Continue sending messages

        ACKMessage ack = (ACKMessage) receivedMessage.getMessage();
        return sendMessage.getId() != ack.getReferId();
    }


    public void sp2pSend(LinkMessage message) throws IOException {
        // Retransmit Forever algorithm with ACK
        int count = 0;

        do {
            count++;
            System.err.println("SL: Sending pool of " + POOL_SIZE + "messages...");
            for (int i = 0; i < POOL_SIZE; i++)
                _flInstance.flp2pSend(message);

        } while(timeout(message));


        System.err.println("SL: ACK verified after " + count + " attempts!");
    }

    public LinkMessage sp2pDeliver() {
        LinkMessage message = _flInstance.flp2pDeliver();

        // Sending ACK to sender as a stop point

        // Creating ACK for the message
        ACKMessage ack = new ACKMessage(message.getId());
        LinkMessage ackMessage = new LinkMessage(ack, message.getEndHostAddress(), message.getEndHostPort());

        // Using fair loss link to send the ACK
        _flInstance.flp2pSend(ackMessage);
        System.err.printf("SL: %s-ACK sent to %s:%d %n", message.getId(), message.getEndHostAddress().getHostName(), message.getEndHostPort());

        return message;
    }

    public void close() {
        _flInstance.close();
    }

}
