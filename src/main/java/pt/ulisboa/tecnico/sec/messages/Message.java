package pt.ulisboa.tecnico.sec.messages;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;

import pt.ulisboa.tecnico.sec.blockchain.BlockchainNode;
import pt.ulisboa.tecnico.sec.crypto.AuthenticationHandler;

public abstract class Message {
    
    public static final String HASH_NONE = "";

    public enum MessageType {
        BFT,
        PROPAGATE_CHANGES,
        CLIENT_REQUEST,
        CLIENT_RESPONSE,
        ACK
    }

    protected MessageType msgType;
    
    // Authentication hashes of the message
    protected String mac = HASH_NONE;
    protected String signature = HASH_NONE;

    protected Message(MessageType msgType) {
        this.msgType = msgType;
    }

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
    public abstract Message fromDataInputStream(DataInputStream dis) throws IOException; 


    public static Message fromByteArray(byte[] bytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bais);

        // spaghetti code because java doesn't allow abstract static methods lol
        MessageType messageType = MessageType.values()[dis.readInt()];
        Message message;
        switch (messageType) {
            case BFT:
                message = new BFTMessage<BlockchainNode>(BlockchainNode.class).fromDataInputStream(dis);
                break;
            case CLIENT_REQUEST:
                message = new ClientRequestMessage().fromDataInputStream(dis);
                break;
            case CLIENT_RESPONSE:
                message = new ClientResponseMessage().fromDataInputStream(dis);
                break;
            case ACK:
                message = new ACKMessage().fromDataInputStream(dis);
                break;
            case PROPAGATE_CHANGES:
                message = new PropagateChangesMessage().fromDataInputStream(dis);
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
            mac = AuthenticationHandler.getMessageMAC(key, this.getDataBytes());
        } catch (IllegalStateException | IOException e) {
            throw new IllegalStateException(String.format("[ERROR] Setting MAC for message %s with %s", this, key));
        }
    }

    public boolean hasValidMAC(SecretKey key) {
        boolean valid = false;
        try {
            valid = AuthenticationHandler.checkMAC(key, this.getMAC(), this.getDataBytes());
        } catch (IllegalStateException | IOException e) {
            throw new IllegalStateException(String.format("[ERROR] Verifying MAC of message %s with %s", this, key));
        }
        return valid;
    }

    public void signMessage(PrivateKey key) throws IllegalStateException {

        try {
            signature = AuthenticationHandler.signBytes(key, this.getDataBytes());
        } catch (IllegalStateException | IOException e) {
            throw new IllegalStateException(String.format("[ERROR] Signing message %s with %s", this, key));
        }
    }

    public boolean hasValidSignature(PublicKey key) throws IllegalStateException {
        boolean valid = false;
        try {
            valid = AuthenticationHandler.checkSignature(key, this.getSignature(), this.getDataBytes());
        } catch (IllegalStateException | IOException e) {
            throw new IllegalStateException(String.format("[ERROR] Verifying signature of message %s with %s", this, key));
        }
        return valid;
    }

    public abstract byte[] getDataBytes() throws IOException;

    @Override
    public abstract String toString();
}
