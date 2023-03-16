package pt.tecnico.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientResponseMessage extends Message {

    public enum Status {
        OK,
        NOT_FOUND,
        REJECTED
    }

    private Status status;
    private Integer timestamp;

    public Status getStatus() {
        return this.status;
    }

    public Integer getTimestamp() {
        return timestamp;
    }

    public ClientResponseMessage(Status status, Integer timestamp) {
        this.status = status;
        this.timestamp = timestamp;
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(Message.MessageType.CLIENT_RESPONSE.ordinal());

        dos.writeInt(status.ordinal());
        dos.writeInt(timestamp);
        if (signature != null) {
            dos.writeInt(super.signature.length);
            dos.write(super.signature);
        }
        return baos.toByteArray();
    }

    public static ClientResponseMessage fromDataInputStream(DataInputStream dis) throws IOException {
        Status status = Status.values()[dis.readInt()];
        int timestamp = dis.readInt();
        int length = dis.readInt();
        byte[] signature = new byte[length];
        dis.readFully(signature);

        return (ClientResponseMessage) new ClientResponseMessage(status, timestamp).setSignature(signature);
    }

    // TODO add something that makes any 2 messages always diferent
    protected byte[] getDataBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(Message.MessageType.CLIENT_RESPONSE.ordinal());
        dos.writeInt(status.ordinal());
        dos.writeInt(timestamp);

        return baos.toByteArray();
    }

    @Override
    public String toString() {
        return String.format("%s/%s/%d", Message.MessageType.CLIENT_RESPONSE.toString(), status, timestamp);
    }
}
