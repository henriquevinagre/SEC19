package pt.ulisboa.tecnico.sec.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import pt.ulisboa.tecnico.sec.ibft.IBFTValueIT;

public class BFTMessage<T extends IBFTValueIT> extends Message {

    private Class<T> clazz;

    public enum Type {
        PRE_PREPARE,
        PREPARE,
        COMMIT
    }

    private Type type;
    private int instance;
    private int round;
    private T value;

    protected BFTMessage(Class<T> clazz) {
        super(MessageType.BFT);
        this.clazz = clazz;
    }

    @SuppressWarnings("unchecked")
    public BFTMessage(Type type, int instance, int round, T value) {
        this((Class<T>) value.getClass());
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

    public T getValue() {
        return this.value;
    }

    protected T getEmptyT() {
        T res = null;
        try {
            res = clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
        
        return res;
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

    public BFTMessage<T> fromDataInputStream(DataInputStream dis) throws IOException {
        this.type = Type.values()[dis.readInt()];
        this.instance = dis.readInt();
        this.round = dis.readInt();
        this.value = getEmptyT().fromDataInputStream(dis);

        return this;
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

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BFTMessage)) return false;
        BFTMessage<T> message = (BFTMessage<T>) obj;
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
