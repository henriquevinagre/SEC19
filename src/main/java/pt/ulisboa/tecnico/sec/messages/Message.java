package pt.ulisboa.tecnico.sec.messages;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;

import pt.ulisboa.tecnico.sec.crypto.AuthenticationHandler;

public abstract class Message {
    
    public static final String HASH_NONE = "";

    public enum MessageType {
        BFT,
        CLIENT_REQUEST,
        CLIENT_RESPONSE,
        ACK
    }

    protected MessageType msgType;
    
    // Authentication hashes of the message
    protected String mac = HASH_NONE;
    protected String signature = HASH_NONE;

    public MessageType getMessageType() {
        return msgType;
    }

    public String getMAC() {
        return mac;
    }

    public String getSignature() {
        return signature;
    }

    protected void setMAC(String mac) {
        this.mac = mac;
    }

    protected void setSignature(String signature) {
        this.signature = signature;
    }

    public abstract byte[] toByteArray() throws IOException;


    public static Message fromByteArray(byte[] bytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bais);

        // spaghetti code because java doesn't allow abstract static methods lol
        MessageType messageType = MessageType.values()[dis.readInt()];
        Message message;
        switch (messageType) {
            case BFT:
                message = BFTMessage.fromDataInputStream(dis);
                message.msgType = MessageType.BFT;
                break;
            case CLIENT_REQUEST:
                message = ClientRequestMessage.fromDataInputStream(dis);
                message.msgType = MessageType.CLIENT_REQUEST;
                break;
            case CLIENT_RESPONSE:
                message = ClientResponseMessage.fromDataInputStream(dis);
                message.msgType = MessageType.CLIENT_RESPONSE;
                break;
            case ACK:
                message = ACKMessage.fromDataInputStream(dis);
                message.msgType = MessageType.ACK;
                break;
            default:
                throw new IllegalArgumentException("Unknown message type: " + messageType);
        }
        
        // Setting mac
        message.mac = dis.readUTF();

        // Setting signature
        message.signature = dis.readUTF();

        return message;
    }

    public void setMessageMAC(SecretKey key) throws IllegalStateException {

        try {
            mac = AuthenticationHandler.getMessageMAC(key, this);
        } catch (IllegalStateException ise) {
            throw new IllegalStateException(String.format("[ERROR] Setting MAC for message %s with %s", this, key));
        }
    }

    public boolean hasValidMAC(SecretKey key) {
        boolean valid = false;
        try {
            valid = AuthenticationHandler.checkMAC(key, this);
        } catch (IllegalStateException ise) {
            throw new IllegalStateException(String.format("[ERROR] Verifying MAC of message %s with %s", this, key));
        }
        return valid;
    }

    public void signMessage(PrivateKey key) throws IllegalStateException {

        try {
            signature = AuthenticationHandler.getMessageSignature(key, this);
        } catch (IllegalStateException ise) {
            throw new IllegalStateException(String.format("[ERROR] Signing message %s with %s", this, key));
        }
    }

    public boolean hasValidSignature(PublicKey key) throws IllegalStateException {
        boolean valid = false;
        try {
            valid = AuthenticationHandler.checkSignature(key, this);
        } catch (IllegalStateException ise) {
            throw new IllegalStateException(String.format("[ERROR] Verifying signature of message %s with %s", this, key));
        }
        return valid;
    }

    public abstract byte[] getDataBytes() throws IOException;

    @Override
    public abstract String toString();
}
