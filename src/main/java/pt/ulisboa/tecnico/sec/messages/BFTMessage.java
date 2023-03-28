package pt.ulisboa.tecnico.sec.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import pt.ulisboa.tecnico.sec.ibft.BlockchainNode;

public class BFTMessage extends Message {
    public enum Type {
        PRE_PREPARE,
        PREPARE,
        COMMIT
    }

    private Type type;
    private int instance;
    private int round;
    private BlockchainNode value;

    public BFTMessage(Type type, int instance, int round, BlockchainNode value) {
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

    public BlockchainNode getValue() {
        return this.value;
    }

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        byte[] node = value.toByteArray();

        dos.writeInt(Message.MessageType.BFT.ordinal());

        dos.writeInt(type.ordinal());
        dos.writeInt(instance);
        dos.writeInt(round);
        dos.write(node);
        dos.writeUTF(super.mac);
        dos.writeUTF(super.signature);

        return baos.toByteArray();
    }

    public static BFTMessage fromDataInputStream(DataInputStream dis) throws IOException {
        Type type = Type.values()[dis.readInt()];
        int instance = dis.readInt();
        int round = dis.readInt();
        BlockchainNode node = BlockchainNode.fromDataInputStream(dis);

        return new BFTMessage(type, instance, round, node);
    }

    public byte[] getDataBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(Message.MessageType.BFT.ordinal());

        dos.writeInt(type.ordinal());
        dos.writeInt(instance);
        dos.writeInt(round);
        dos.write(value.toByteArray());

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

    @Override
    public int hashCode() {
        int result = 17;

        result = 31 * result + value.hashCode();
        result = 31 * result + instance;
        result = 31 * result + round;

        return result;
    }

}
