package pt.tecnico.messages;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

public abstract class Message {
    
    public enum MessageType {
        BFT,
        CLIENT,
        ACK
    }

    protected MessageType msgType;
    
    protected byte[] signature;

    public MessageType getMessageType() {
        return msgType;
    }

    protected Message setSignature(byte[] signature) {
        this.signature = signature;
        return this;
    }

    // TODO: embed signature here
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
            case CLIENT:
                message = ClientMessage.fromDataInputStream(dis);
                message.msgType = MessageType.CLIENT;
                break;
            case ACK:
                message = ACKMessage.fromDataInputStream(dis);
                message.msgType = MessageType.ACK;
                break;
            default:
                throw new IllegalArgumentException("Unknown message type: " + messageType);
        }

        return message;
    }

    public void signMessage(PrivateKey key) {
        Signature newSignature;
        try {
            newSignature = Signature.getInstance("SHA256withRSA");
            newSignature.initSign(key);
            newSignature.update(this.getDataBytes());
            signature = newSignature.sign();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasValidSignature(PublicKey key) {
        boolean valid = false;
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key);
            verifier.update(this.getDataBytes());
            valid = verifier.verify(signature);
        } catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException | IOException e) {
            e.printStackTrace();
        }
        return valid;
    }

    protected abstract byte[] getDataBytes() throws IOException;

    @Override
    public abstract String toString();
}
