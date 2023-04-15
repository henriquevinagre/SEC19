package pt.ulisboa.tecnico.sec.tes.transactions;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.PublicKey;

import pt.ulisboa.tecnico.sec.crypto.KeyHandler;
import pt.ulisboa.tecnico.sec.tes.TESAccount;
import pt.ulisboa.tecnico.sec.tes.TESState;

public class TransferTransaction extends Transaction {

    private PublicKey destination;
    private double amount;

    public TransferTransaction(PublicKey source, PublicKey destination, double amount) {
        super(TESOperation.TRANSFER, source);
        this.destination = destination;
        this.amount = amount;
    }

    // In-progress transaction
    private TransferTransaction() {
        super(TESOperation.TRANSFER);
    }

    private void setDestination(PublicKey key) { this.destination = key; }
    private void setAmount(double amount) { this.amount = amount; }

    public PublicKey getDestination() { return destination; }
    public String getDestinationBase64() { return KeyHandler.KeyBase64(destination); }
    public String getDestinationBase64Readable() { return KeyHandler.KeyBase64Readable(destination); }
    public double getAmount() { return amount; }


    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // serialize operation
        dos.writeInt(super.getOperation().ordinal());

        // serialize destination key
        byte[] destinationBytes = this.getDestination().getEncoded();
        dos.writeInt(destinationBytes.length);
        dos.write(destinationBytes);

        // serialize amount
        dos.writeDouble(amount);

        // serialize source key
        byte[] sourceBytes = super.getSource().getEncoded();
        dos.writeInt(sourceBytes.length);
        dos.write(sourceBytes);
        
        // serialize nonce and challenge
        dos.writeInt(super.getNonce());
        String challenge = super.getChallenge();
        dos.writeUTF(challenge);

        return baos.toByteArray();
    }

    public static TransferTransaction fromDataInputStream(DataInputStream dis) throws IOException {
        
        TransferTransaction transaction = new TransferTransaction();

        byte[] destinationBytes = new byte[dis.readInt()];
        dis.readFully(destinationBytes);

        PublicKey destination = null;

        try {
            destination = KeyHandler.deserializePublicKey(destinationBytes);
        } catch (IllegalStateException ile) {
            throw new IllegalStateException("[ERROR] Deserializing transaction destination key");
        }

        transaction.setDestination(destination);
        
        transaction.setAmount(dis.readDouble());        

        return transaction;
    }

    public byte[] getDataBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // serialize operation
        dos.writeInt(super.getOperation().ordinal());

        // serialize destination key
        byte[] destinationBytes = this.getDestination().getEncoded();
        dos.writeInt(destinationBytes.length);
        dos.write(destinationBytes);

        // serialize amount
        dos.writeDouble(amount);

        // serialize source key
        byte[] sourceBytes = super.getSource().getEncoded();
        dos.writeInt(sourceBytes.length);
        dos.write(sourceBytes);

        // serialize nonce
        dos.writeInt(super.getNonce());

        return baos.toByteArray();
    }

    @Override
    public boolean checkSyntax() {
        return amount > 0 && amount < Double.MAX_VALUE && // Amount should be between 0 and DOUBLE_MAX
            !getSource().equals(destination); // Can't transfer to ourself :)
    }

    @Override
    public boolean updateTESState(TESState state) {
        TESAccount sourceAccount = state.getAccount(this.getSource());
        if (sourceAccount == null) return false;
        TESAccount destinationAccount = state.getAccount(this.getDestination());
        if (destinationAccount == null) return false;

        if (sourceAccount.getTucs() < amount || amount >= Double.MAX_VALUE - destinationAccount.getTucs()) {
            return false;
        }

        sourceAccount.subtractBalance(amount);
        destinationAccount.addBalance(amount);

        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TransferTransaction)) return false;
        TransferTransaction t = (TransferTransaction) obj;
        return super.equals(t) 
            && this.getDestination().equals(t.getDestination())
            && this.getAmount() == t.getAmount();
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();

        result = 31 * result + destination.hashCode();
        result = 31 * result + (int) amount;
        
        return result;
    }

    @Override
    public String toString() {
        return String.format("{op: TRANSFER, source: %s, destination: %s, amount: %.4f}", 
            getSourceBase64Readable(), getDestinationBase64Readable(), amount);
    }
}
