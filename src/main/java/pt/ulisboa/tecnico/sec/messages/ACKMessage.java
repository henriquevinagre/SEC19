package pt.ulisboa.tecnico.sec.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ACKMessage extends Message {

    private int referId;

    public int getReferId() {
        return referId;
    }

    public ACKMessage() {
        super(MessageType.ACK);
    }

    public ACKMessage(int referId) {
        super(MessageType.ACK);
        this.referId = referId;
    }

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(Message.MessageType.ACK.ordinal());

        dos.writeInt(referId);
        dos.writeUTF(super.mac);
        dos.writeUTF(super.signature);

        return baos.toByteArray();
    }

    public ACKMessage fromDataInputStream(DataInputStream dis) throws IOException {
        this.referId = dis.readInt();

        return this;
    }

    public byte[] getDataBytes() throws IOException {
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
