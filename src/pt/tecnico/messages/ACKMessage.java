package pt.tecnico.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ACKMessage extends Message {

    private final String referId;

    public String getReferId() {
        return referId;
    }

    public ACKMessage(String referId) {
        this.referId = referId;
    }

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(Message.MessageType.ACK.ordinal());

        dos.writeUTF(referId);
        return baos.toByteArray();
    }

    public static ACKMessage fromDataInputStream(DataInputStream dis) throws IOException {
        String referId = dis.readUTF();
        return new ACKMessage(referId);
    }

    // TODO add something that makes any 2 messages always diferent
    protected byte[] getDataBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(Message.MessageType.ACK.ordinal());
        dos.writeUTF(referId);
        return baos.toByteArray();
    }
}
