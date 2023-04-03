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

    public ClientResponseMessage() {
        super(MessageType.CLIENT_RESPONSE);
    }

    public ClientResponseMessage(Status status, Integer timestamp) {
        super(MessageType.CLIENT_RESPONSE);
        this.status = status;
        this.timestamp = timestamp;
    }

    public Status getStatus() {
        return this.status;
    }

    public Integer getTimestamp() {
        return timestamp;
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

    @Override
    public ClientResponseMessage fromDataInputStream(DataInputStream dis) throws IOException {
        this.status = Status.values()[dis.readInt()];
        this.timestamp = dis.readInt();

        return this;
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
