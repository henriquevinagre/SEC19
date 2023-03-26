package pt.ulisboa.tecnico.sec.messages;

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
        dos.writeUTF(super.mac);
        dos.writeUTF(super.signature);

        return baos.toByteArray();
    }

    public static ClientResponseMessage fromDataInputStream(DataInputStream dis) throws IOException {
        Status status = Status.values()[dis.readInt()];
        int timestamp = dis.readInt();

        return new ClientResponseMessage(status, timestamp);
    }

    public byte[] getDataBytes() throws IOException {
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
