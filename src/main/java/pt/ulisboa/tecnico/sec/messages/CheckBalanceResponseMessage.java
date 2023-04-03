package pt.ulisboa.tecnico.sec.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CheckBalanceResponseMessage extends ClientResponseMessage {


    private double balance;

    public CheckBalanceResponseMessage(Status status, Integer timestamp, double balance) {
        super(status, timestamp);
        this.balance = balance;
    }



    public double getBalance() {
        return balance;
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.write(super.toByteArray());
        dos.writeDouble(balance);
        
        return baos.toByteArray();
    }

    @Override
    public CheckBalanceResponseMessage fromDataInputStream(DataInputStream dis) throws IOException {
        super.fromDataInputStream(dis);
        this.balance = dis.readDouble();

        return this;
    }

    public byte[] getDataBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.write(super.getDataBytes());
        dos.writeDouble(balance);
        
        return baos.toByteArray();
    }

    @Override
    public String toString() {
        return String.format("%s/%s/%.2f/%d", Message.MessageType.CLIENT_RESPONSE.toString(), this.getStatus(), this.getBalance(), this.getTimestamp());
    }
}
