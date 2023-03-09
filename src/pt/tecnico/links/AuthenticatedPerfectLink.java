package pt.tecnico.links;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

import pt.tecnico.instances.HDLProcess;
import pt.tecnico.messages.LinkMessage;

// Authenticated Perfect point to point link using Perfect links
public class AuthenticatedPerfectLink {
    
    private PerfectLink _plInstance;
    private PrivateKey _selfPrivKey;
    private PublicKey _endHostPubKey;

    // Assuming the same end host public key for each process (client or server) - using signatures
    // Maybe MAC instead
    public AuthenticatedPerfectLink(HDLProcess p, PrivateKey selfPrivKey, PublicKey endHostPubKey) {
        _plInstance = new PerfectLink(p);
        _selfPrivKey = selfPrivKey;
        _endHostPubKey = endHostPubKey;
    }

    public void alp2pSend(LinkMessage message) throws IOException {
        System.err.println("APL: Signing message with id: " + message.getId() + "...");
        message.getMessage().signMessage(_selfPrivKey);
        _plInstance.pp2pSend(message);
    }

    public LinkMessage alp2pDeliver() {
        LinkMessage message;

        // Wait for a message that was not delivered yet with valid signature
        do {
            message = _plInstance.pp2pDeliver();
            System.err.println("APL: Received message with id: " + message.getId());
        } while (!message.getMessage().hasValidSignature(_endHostPubKey));

        assert(message != null);

        System.err.println("APL: Message signature verified! Delivering message...");

        return message;
    }

    public void close() {
        _plInstance.close();
    }

}
