package pt.ulisboa.tecnico.sec.tes.transactions;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.util.Base64;

import pt.ulisboa.tecnico.sec.crypto.KeyHandler;
import pt.ulisboa.tecnico.sec.tes.TESAccount;
import pt.ulisboa.tecnico.sec.tes.TESState;

public class TransferTransaction extends Transaction {

    private PublicKey _destination;
    private double _amount;

    public TransferTransaction(PublicKey source, PublicKey destination, double amount) {
        super(TESOperation.TRANSFER, source);
        _destination = destination;
        _amount = amount;
    }

    // In-progress transaction
    private TransferTransaction() { super(TESOperation.TRANSFER); }

    private void setDestination(PublicKey key) { _destination = key; }
    private void setAmount(double amount) { _amount = amount; }

    public PublicKey getDestination() { return _destination; }
    public String getDestinationB64() { return Base64.getEncoder().encodeToString(_destination.getEncoded()); }
    public double getAmount() { return _amount; }


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
        dos.writeDouble(_amount);

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
        dos.writeDouble(_amount);

        // serialize source key
        byte[] sourceBytes = super.getSource().getEncoded();
        dos.writeInt(sourceBytes.length);
        dos.write(sourceBytes);

        // serialize nonce
        dos.writeInt(super.getNonce());

        return baos.toByteArray();
    }

    @Override
    public boolean updateTESState(TESState state) {
        TESAccount sourceAccount = state.getAccount(this.getSource());
        if (sourceAccount == null) return false;
        TESAccount destinationAccount = state.getAccount(this.getDestination());
        if (destinationAccount == null) return false;

        if (sourceAccount.getTucs() < _amount || _amount >= Double.MAX_VALUE - destinationAccount.getTucs()) {
            return false;
        }

        sourceAccount.subtractBalance(_amount);
        destinationAccount.addBalance(_amount);

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

        result = 31 * result + _destination.hashCode();
        result = 31 * result + (int) _amount;
        
        return result;
    }

    @Override
    public String toString() {
        return String.format("{op: TRANSFER, source: %s, destination: %s, amount: %.2f}", 
            this.getSourceB64().substring(46, 62), this.getDestinationB64().substring(46, 62), this.getAmount());
    }
}
