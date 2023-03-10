package pt.tecnico.links;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

import pt.tecnico.instances.HDLProcess;
import pt.tecnico.messages.LinkMessage;

// Authenticated Perfect point to point link using Perfect links
public class AuthenticatedPerfectLink {
    
    private PerfectLink plInstance;
    private PrivateKey selfPrivKey;

    // Assuming the same end host public key for each process (client or server) - using signatures
    // Maybe MAC instead
    public AuthenticatedPerfectLink(HDLProcess p) {
        plInstance = new PerfectLink(p);
        selfPrivKey = p.getPrivateKey();
    }

    public void alp2pSend(LinkMessage message) throws IOException {
        System.err.println("APL: Signing message with id: " + message.getId() + "...");
        message.getMessage().signMessage(selfPrivKey);
        plInstance.pp2pSend(message);
    }

    public LinkMessage alp2pDeliver() {
        LinkMessage message;
        PublicKey senderKey;

        // Wait for a message that was not delivered yet with valid signature
        do {
            message = plInstance.pp2pDeliver();
            senderKey = message.getEndHost().getPublicKey();
            System.err.println("APL: Received message with id: " + message.getId());
        } while (!message.getMessage().hasValidSignature(senderKey));

        assert(message != null);

        System.err.println("APL: Message signature verified! Delivering message...");

        return message;
    }

    public void close() {
        plInstance.close();
    }

}
