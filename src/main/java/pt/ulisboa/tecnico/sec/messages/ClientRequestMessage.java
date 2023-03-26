package pt.ulisboa.tecnico.sec.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientRequestMessage extends Message {

    private String value;

    public String getValue() {
        return value;
    }

    public ClientRequestMessage(String value) {
        this.value = value;
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(Message.MessageType.CLIENT_REQUEST.ordinal());
        dos.writeUTF(value);
        dos.writeUTF(super.mac);
        dos.writeUTF(super.signature);

        return baos.toByteArray();
    }

    public static ClientRequestMessage fromDataInputStream(DataInputStream dis) throws IOException {
        String value = dis.readUTF();

        return new ClientRequestMessage(value);
    }

    public byte[] getDataBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(Message.MessageType.CLIENT_REQUEST.ordinal());
        dos.writeUTF(value);

        return baos.toByteArray();
    }

    @Override
    public String toString() {
        return String.format("%s/%s", Message.MessageType.CLIENT_REQUEST.toString(), value);
    }
}
