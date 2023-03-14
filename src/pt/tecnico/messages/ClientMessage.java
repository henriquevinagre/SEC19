package pt.tecnico.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientMessage extends Message {
    public enum Type {
        REQUEST,
        RESPONSE
    }

    public enum Status {
        OK,
        NOT_FOUND,
        REJECTED
    }

    private Type type;
    private Status status;
    private String value;

    public Type getType() {
        return type;
    }

    public Status getStatus() {
        return status;
    }

    public String getValue() {
        return value;
    }

    public ClientMessage(Type type, String value) {
        this.type = type;
        this.value = value;
        this.status = Status.OK;
    }

    public ClientMessage(Type type, Status status) {
        this.type = type;
        this.status = status;
        this.value = "";
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(Message.MessageType.CLIENT.ordinal());

        dos.writeInt(type.ordinal());
        dos.writeInt(status.ordinal());
        dos.writeUTF(value);
        if (signature != null) {
            dos.writeInt(super.signature.length);
            dos.write(super.signature);
        }
        return baos.toByteArray();
    }

    public static ClientMessage fromDataInputStream(DataInputStream dis) throws IOException {
        Type type = Type.values()[dis.readInt()];
        Status status = Status.values()[dis.readInt()];
        String value = dis.readUTF();

        int length = dis.readInt();
        byte[] signature = new byte[length];
        dis.readFully(signature);

        if(type.equals(Type.REQUEST)) {
            return (ClientMessage) new ClientMessage(type, value).setSignature(signature);
        } else {
            return (ClientMessage) new ClientMessage(type, status).setSignature(signature);
        }
    }

    // TODO add something that makes any 2 messages always diferent
    protected byte[] getDataBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(Message.MessageType.ACK.ordinal());

        dos.writeInt(type.ordinal());
        dos.writeInt(status.ordinal());
        dos.writeUTF(value);

        return baos.toByteArray();
    }

    @Override
    public String toString() {
        return String.format("Client/%s/%s:%s", type.toString(), status.toString(), value); 
    }

}
