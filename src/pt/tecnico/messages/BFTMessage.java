package pt.tecnico.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BFTMessage extends Message {
    public enum Type {
        PRE_PREPARE,
        PREPARE,
        COMMIT,
        ROUND_CHANGE
    }

    private Type type;
    private int instance;
    private int round;
    private String value;

    // TODO another constructor is needed for ROUND_CHANGE messages (second stage of the project)
    public BFTMessage(Type type, int instance, int round, String value) {
        this.type = type;
        this.instance = instance;
        this.round = round;
        this.value = value;
    }

    public Type getType() {
        return this.type;
    }

    public int getInstance() {
        return this.instance;
    }

    public int getRound() {
        return this.round;
    }

    public String getValue() {
        return this.value;
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(Message.MessageType.BFT.ordinal());

        dos.writeInt(type.ordinal());
        dos.writeInt(instance);
        dos.writeInt(round);
        dos.writeUTF(value);
        dos.writeInt(super.signature.length);
        dos.write(super.signature);

        return baos.toByteArray();
    }

    public static BFTMessage fromDataInputStream(DataInputStream dis) throws IOException {
        Type type = Type.values()[dis.readInt()];
        int instance = dis.readInt();
        int round = dis.readInt();
        String value = dis.readUTF();

        int length = dis.readInt();
        byte[] signature = new byte[length];
        dis.readFully(signature);

        return (BFTMessage) new BFTMessage(type, instance, round, value).setSignature(signature);
    }

    // TODO add something that makes any 2 messages always diferent
    protected byte[] getDataBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(Message.MessageType.BFT.ordinal());

        dos.writeInt(type.ordinal());
        dos.writeInt(instance);
        dos.writeInt(round);
        dos.writeUTF(value);

        return baos.toByteArray();
    }

    @Override
    public String toString() {
        return String.format("BFT/%s(%d, %d):%s", type.toString(), instance, round, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BFTMessage)) return false;
        BFTMessage message = (BFTMessage) obj;
        return message.getInstance() == this.instance && message.getRound() == this.round
                && message.getType().equals(this.getType()) && message.getValue().equals(this.getValue());
    }

}
