package pt.tecnico.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ACKMessage extends Message {

    private final int referId;

    public int getReferId() {
        return referId;
    }

    public ACKMessage(int referId) {
        this.referId = referId;
    }

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(Message.MessageType.ACK.ordinal());

        dos.writeInt(referId);
        if (super.signature != null) {
            dos.writeInt(super.signature.length);
            dos.write(super.signature);
        } else {
            dos.writeInt(0); // No signature, therefore length 0
        }

        return baos.toByteArray();
    }

    public static ACKMessage fromDataInputStream(DataInputStream dis) throws IOException {
        int referId = dis.readInt();

        int length = dis.readInt();
        if (length != 0) {
            byte[] signature = new byte[length];
            dis.readFully(signature);

            return (ACKMessage) new ACKMessage(referId).setSignature(signature);
        } else {
            return (ACKMessage) new ACKMessage(referId);
        }
    }

    // TODO add something that makes any 2 messages always diferent
    protected byte[] getDataBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(Message.MessageType.ACK.ordinal());
        dos.writeInt(referId);
        return baos.toByteArray();
    }

    @Override
    public String toString() {
        return String.format("ACK:%d", referId); 
    }
}
