package pt.ulisboa.tecnico.sec.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import pt.ulisboa.tecnico.sec.tes.transactions.Transaction;

public class ClientRequestMessage extends Message {

    private Transaction transaction;

    public ClientRequestMessage() {
        super(MessageType.CLIENT_REQUEST);
    }

    public ClientRequestMessage(Transaction transaction) {
        super(MessageType.CLIENT_REQUEST);
        this.transaction = transaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        byte[] transactionBytes = transaction.toByteArray();

        dos.writeInt(Message.MessageType.CLIENT_REQUEST.ordinal());
        dos.writeInt(transactionBytes.length);
        dos.write(transactionBytes);
        dos.writeUTF(super.mac);
        dos.writeUTF(super.signature);

        return baos.toByteArray();
    }

    @Override
    public ClientRequestMessage fromDataInputStream(DataInputStream dis) throws IOException {
        byte[] transactionBytes = new byte[dis.readInt()];
        dis.readFully(transactionBytes);
        this.transaction = Transaction.fromByteArray(transactionBytes);

        return this;
    }

    public byte[] getDataBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        byte[] transactionBytes = transaction.toByteArray();

        dos.writeInt(Message.MessageType.CLIENT_REQUEST.ordinal());
        dos.writeInt(transactionBytes.length);
        dos.write(transactionBytes);

        return baos.toByteArray();
    }

    @Override
    public String toString() {
        return String.format("%s/%s", Message.MessageType.CLIENT_REQUEST.toString(), transaction.toString());
    }
}
