package pt.tecnico.links;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.ArrayList;
import java.util.List;

import pt.tecnico.instances.HDLProcess;
import pt.tecnico.messages.ACKMessage;
import pt.tecnico.messages.LinkMessage;
import pt.tecnico.messages.Message;

// Stubborn point to point link using Fair loss links
public class StubbornLink {

    private static final int POOL_SIZE = 1;
    private static final int TIMEOUT_MS = 500;

    private Thread deliverThread = new Thread(() -> { try {
        continuousDeliver();
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }});

    private FairLossLink _flInstance;

    private HDLProcess channelOwner;

    private List<LinkMessage> acks = new ArrayList<>();
    private List<LinkMessage> messages = new ArrayList<>();

    public StubbornLink(HDLProcess p) {
        channelOwner = p;
        _flInstance = new FairLossLink(p);
        deliverThread.start();
    }

    private void continuousDeliver() throws IOException {
       while (true) {
           LinkMessage delivered = _flInstance.flp2pDeliver();
           System.err.println("msg_id = " + delivered.getId() + ", from " + delivered.getSender().getID() + ", to " + delivered.getReceiver().getID());
           System.err.println("SL: Continuous Deliver: " + delivered.getMessage().getMessageType());

           if (delivered.getMessage().getMessageType() == Message.MessageType.ACK) {
                synchronized (acks) {
                    System.err.println("SL: Ack added to list");
                    acks.add(delivered);
                    acks.notifyAll();
                }
           }
           else {
                synchronized (messages) {
                    System.err.println("SL: Message added to list");
                    messages.add(delivered);
                    messages.notifyAll();
                }
           }

           if (delivered.getTerminate()) break; // terminating 🤖🤖🤖
       }
    }

    private LinkMessage getAckMessage(int referId) throws InterruptedException {
        System.err.println("SL: TRYING ACK-" + referId + " retrieved");
        while (true) {
            synchronized (acks) {
                if (!acks.isEmpty()) {
                    for (int i = acks.size()-1; i >= 0; i--) {
                        if (((ACKMessage) acks.get(i).getMessage()).getReferId() == referId ) {
                            System.err.println("SL: ACK-" + referId + " retrieved");
                            return acks.remove(i);
                        }
                    }
                } else {
                    acks.wait();
                }
            }
        }
    }

    private LinkMessage getMessage() throws InterruptedException {
        synchronized (messages) {
            while (true) {
                if (!messages.isEmpty()) {
                    System.err.println("SL: Message retrieved from list");
                    return messages.remove(0);
                }
                else
                    messages.wait();
            }
        }
    }

    private boolean timeout(LinkMessage sendMessage) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<LinkMessage> future = executor.submit(new Callable<LinkMessage>() {
                public LinkMessage call() throws IOException {
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

    private boolean timeout(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public void sp2pSend(LinkMessage message) throws IOException {
        // Retransmit Forever algorithm with ACK
        int count = 0;

        Thread thread = new Thread (() -> {
            try {
                this.getAckMessage(message.getId());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        thread.start();

        do {
            if (!thread.isAlive()) break;
            count++;
            System.err.println("SL: Sending pool of " + POOL_SIZE + " messages...");
            for (int i = 0; i < POOL_SIZE; i++)
                _flInstance.flp2pSend(message);
            
            if (message.getTerminate()) {
                thread.interrupt(); break;
            }
            
        // First timeout is bogus, just wait a bit to check the ack :)
        } while (count <= 5 && timeout(500) && thread.isAlive() && timeout(TIMEOUT_MS));

        thread.interrupt();


        System.err.println("SL: ACK verified after " + count + " attempts!");
    }


    public LinkMessage sp2pDeliver() throws IOException, InterruptedException {
        LinkMessage message = null;

        // Wait for a response message that is not an ACK
        message = this.getMessage();
        System.err.println("SL: Received message with id: " + message.getId());

        assert(message != null);

        if (message.getTerminate())
            return message;

        // Sending ACK to sender as a stop point

        // Creating ACK for the message
        ACKMessage ack = new ACKMessage(message.getId());
        LinkMessage ackMessage = new LinkMessage(ack, this.channelOwner, message.getSender(), false);

        // Using fair loss link to send the ACK
        _flInstance.flp2pSend(ackMessage);
        System.err.printf("SL: %s-ACK sent to %s %n", message.getId(), message.getSender());

        return message;
    }


    public void close() {
        deliverThread.interrupt();
        _flInstance.close();
    }

}
