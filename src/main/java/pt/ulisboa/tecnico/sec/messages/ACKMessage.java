package pt.ulisboa.tecnico.sec.messages;

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
        dos.writeUTF(super.mac);
        dos.writeUTF(super.signature);

        return baos.toByteArray();
    }

    public static ACKMessage fromDataInputStream(DataInputStream dis) throws IOException {
        int referId = dis.readInt();

        return new ACKMessage(referId);
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
