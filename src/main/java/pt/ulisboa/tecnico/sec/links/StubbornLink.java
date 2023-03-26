package pt.ulisboa.tecnico.sec.links;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.sec.ibft.ByzantineHandler;
import pt.ulisboa.tecnico.sec.ibft.HDLProcess;
import pt.ulisboa.tecnico.sec.ibft.ByzantineHandler.ByzantineBehaviour;
import pt.ulisboa.tecnico.sec.messages.ACKMessage;
import pt.ulisboa.tecnico.sec.messages.LinkMessage;
import pt.ulisboa.tecnico.sec.messages.Message;

// Stubborn point to point link using Fair loss links
public class StubbornLink extends Channel {

    private static final int POOL_SIZE = 1;
    private static final long INITIAL_TIMEOUT_MS = 500;
    private static final long TIMEOUT_MULTIPLIER = 2;

    private FairLossLink _flInstance;

    private Thread deliverThread = new Thread(() -> {
        try {
            continuousDeliver();
        } catch (IllegalStateException | InterruptedException e) {
            // just ending deliver
        }
    });

    private List<LinkMessage> acks = new ArrayList<>();
    private List<LinkMessage> messages = new ArrayList<>();

    public StubbornLink(HDLProcess p) {
        super(p);
        _flInstance = new FairLossLink(p);
        deliverThread.start();
    }

    private void continuousDeliver() throws IllegalStateException, InterruptedException {
        while (true) {
            LinkMessage delivered;
            try {
                delivered = _flInstance.deliver();
            } catch (SocketTimeoutException e) {
                break;
            }
            System.err.printf("[%s] SL: Continuous Deliver: %s%n", this.owner, delivered);

            if (delivered.getMessage().getMessageType().equals(Message.MessageType.ACK)) {
                synchronized (acks) {
                    System.err.printf("[%s] SL: Ack added to its list%n", this.owner);
                    acks.add(delivered);
                    acks.notifyAll();
                }
            }
            else {
                synchronized (messages) {
                    System.err.printf("[%s] SL: Message added to its list%n", this.owner);
                    messages.add(delivered);
                    messages.notifyAll();
                }
            }
        }
    }

    private void waitAckMessage(int referId) throws InterruptedException {
        System.err.printf("[%s] SL: TRYING %d-ACK retrieved%n", this.owner, referId);
        while (true) {
            // [B2] Skipping ACKs behaviour
            if (ByzantineHandler.withBehaviourActive(this.owner, ByzantineBehaviour.SKIPPING_ACKS)) {
                System.err.printf("[B2] Server %d skipped acks %n", this.owner.getID());
                return;
            }
            synchronized (acks) {
                if (!acks.isEmpty()) {
                    for (int i = acks.size()-1; i >= 0; i--) {
                        if (((ACKMessage) acks.get(i).getMessage()).getReferId() == referId ) {
                            System.err.printf("[%s] SL: %d-ACK retrieved%n", this.owner, referId);
                            acks.remove(i);
                            return;
                        }
                    }
                } else {
                    acks.wait();
                }
            }
        }
    }

    private LinkMessage getMessage() throws InterruptedException, SocketTimeoutException {
        synchronized (messages) {
            while (true) {
                if (!deliverThread.isAlive()) {
                    throw new SocketTimeoutException();
                }
                if (!messages.isEmpty()) {
                    System.err.printf("[%s] SL: Message retrieved from list%n", this.owner);
                    return messages.remove(0);
                }
                else {
                    messages.wait(INITIAL_TIMEOUT_MS, 1);
                }
            }
        }
    }

    private boolean timeout(long ms) throws IllegalStateException {
        try {
            Thread.sleep(ms, 1);
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    public void send(LinkMessage message) throws IllegalStateException, InterruptedException {
        // Retransmit Forever algorithm with ACK
        int count = 0;

        Thread thread = new Thread (() -> {
            try {
                this.waitAckMessage(message.getId());
            } catch (InterruptedException e) {
                // just stop the thread
            }
        });

        thread.start();

        long timeout_ms = INITIAL_TIMEOUT_MS;

        while (thread.isAlive()) {
            count++;
            
            try {
                System.err.printf("[%s] SL: Sending pool of %d messages...%n", this.owner, POOL_SIZE);
                for (int i = 0; i < POOL_SIZE; i++)
                    _flInstance.send(message);
            } catch (IllegalStateException ile) {
                System.err.printf("[%s] SL: %s%n", this.owner, ile.getMessage());
                if (thread.isAlive()) thread.interrupt();
                throw new IllegalStateException(ile.getMessage());
            }

            if (message.getTerminate()) {
                thread.interrupt(); 
                return;
            }

            if (!timeout(timeout_ms)) {
                thread.interrupt();
                throw new IllegalStateException(String.format("[ERROR] [%s] SL: Timeout interrupted!", this.owner));
            }

            // Exponential timeout
            timeout_ms *= TIMEOUT_MULTIPLIER;
        }

        System.err.printf("[%s] SL: ACK verified after %d attempts!%n", this.owner, count);
    }


    public LinkMessage deliver() throws IllegalStateException, InterruptedException, SocketTimeoutException {
        LinkMessage message = null;

        // Wait for a response message that is not an ACK
        message = this.getMessage();
        System.err.printf("[%s] SL: Received message with id: %d%n", this.owner, message.getId());

        assert(message != null);

        if (message.getTerminate())
            return message;

        // Sending ACK to sender as a stop point

        // Creating ACK for the message
        ACKMessage ack = new ACKMessage(message.getId());
        LinkMessage ackMessage = new LinkMessage(ack, this.owner, message.getSender(), false);

        // Using fair loss link to send the ACK
        try {
            System.err.printf("[%s] SL: Sending %s%n", this.owner, ackMessage);
            _flInstance.send(ackMessage);
        } catch (IllegalStateException ile) {
            ile.printStackTrace();
            // ACK was lost, not a problem since the sender still retransmiting the same message more
        }

        return message;
    }


    public void close() {
        deliverThread.interrupt();
        _flInstance.close();
    }

}
