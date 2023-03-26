package pt.ulisboa.tecnico.sec.links;

import java.net.SocketTimeoutException;

import javax.crypto.SecretKey;

import pt.ulisboa.tecnico.sec.ibft.HDLProcess;
import pt.ulisboa.tecnico.sec.messages.LinkMessage;

// Authenticated Perfect point to point link using Perfect links with MACs
public class AuthenticatedPerfectLink extends Channel {

    private PerfectLink plInstance;

    public AuthenticatedPerfectLink(HDLProcess p) {
        super(p);
        plInstance = new PerfectLink(p);
    }

    public void send(LinkMessage message) throws IllegalStateException, InterruptedException {
        System.err.printf("[%s] APL: Setting MAC to message %s\n", this.owner, message);
        message.getMessage().setMessageMAC(this.owner.getSecretKeyFor(message.getReceiver()));
        plInstance.send(message);
    }

    public LinkMessage deliver() throws IllegalStateException, InterruptedException, SocketTimeoutException {
        LinkMessage message = null;
        SecretKey secretKey = null;

        // Wait for a message that was not delivered yet with valid MAC
        do {
            message = plInstance.deliver();
            secretKey = this.owner.getSecretKeyFor(message.getSender());
            System.err.printf("[%s] APL: Received message %s\n", this.owner, message);
        } while (!message.getTerminate() && !message.getMessage().hasValidMAC(secretKey));

        assert(message != null);

        System.err.printf("[%s] APL: Message MAC verified! Delivering message %d ...\n", this.owner, message.getId());
        return message;
    }

    public void close() {
        plInstance.close();
    }

}
