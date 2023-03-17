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
        if (signature != null) {
            dos.writeInt(super.signature.length);
            dos.write(super.signature);
        }
        return baos.toByteArray();
    }

    public static ClientRequestMessage fromDataInputStream(DataInputStream dis) throws IOException {
        String value = dis.readUTF();

        int length = dis.readInt();
        byte[] signature = new byte[length];
        dis.readFully(signature);

        return (ClientRequestMessage) new ClientRequestMessage(value).setSignature(signature);
    }

    // TODO add something that makes any 2 messages always diferent
    protected byte[] getDataBytes() throws IOException {
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
