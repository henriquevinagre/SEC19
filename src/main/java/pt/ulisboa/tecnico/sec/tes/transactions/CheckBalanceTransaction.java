package pt.ulisboa.tecnico.sec.tes.transactions;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.PublicKey;

import pt.ulisboa.tecnico.sec.tes.TESState;
import pt.ulisboa.tecnico.sec.crypto.KeyHandler;
import pt.ulisboa.tecnico.sec.tes.TESAccount;

public class CheckBalanceTransaction extends Transaction {
    public enum ReadType {
        STRONGLY_CONSISTENT,
        WEAKLY_CONSISTENT
    }

    private PublicKey owner;
    private ReadType readType;

    public CheckBalanceTransaction(PublicKey creator, PublicKey owner, ReadType readType) {
        super(TESOperation.CHECK_BALANCE, creator);
        this.owner = owner;
        this.readType = readType;
    }

    // In-progress transaction
    private CheckBalanceTransaction() {
        super(TESOperation.CHECK_BALANCE);
    }
    
    private void setOwner(PublicKey key) { this.owner = key; }
    private void setReadType(ReadType type) { this.readType = type; }
    public PublicKey getOwner() { return this.owner; }
    public String getOwnerBase64() { return KeyHandler.KeyBase64(owner); }
    public String getOwnerBase64Readable() { return KeyHandler.KeyBase64Readable(owner); }
    public ReadType getReadType() { return this.readType; }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // serialize operation
        dos.writeInt(super.getOperation().ordinal());

        // serialize read type
        dos.writeInt(this.getReadType().ordinal());

        // serialize owner key
        byte[] ownerBytes = this.getOwner().getEncoded();
        dos.writeInt(ownerBytes.length);
        dos.write(ownerBytes);

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

    public static CheckBalanceTransaction fromDataInputStream(DataInputStream dis) throws IOException {
        
        CheckBalanceTransaction transaction = new CheckBalanceTransaction();

        transaction.setReadType(ReadType.values()[dis.readInt()]);

        byte[] ownerBytes = new byte[dis.readInt()];
        dis.readFully(ownerBytes);

        PublicKey owner = null;

        try {
            owner = KeyHandler.deserializePublicKey(ownerBytes);
        } catch (IllegalStateException ile) {
            throw new IllegalStateException("[ERROR] Deserializing transaction destination key");
        }

        transaction.setOwner(owner);

        return transaction;
    }

    public byte[] getDataBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // serialize operation
        dos.writeInt(super.getOperation().ordinal());

        // serialize read type
        dos.writeInt(this.getReadType().ordinal());

        // serialize owner key
        byte[] ownerBytes = this.getOwner().getEncoded();
        dos.writeInt(ownerBytes.length);
        dos.write(ownerBytes);

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
        return true;
    }

    @Override
    public boolean updateTESState(TESState state) {
        if (state.getAccount(this.getSource()) != null) {
            return false;
        }
        TESAccount newAccount = new TESAccount(this.getSource());
		state.addAccount(newAccount);

        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CheckBalanceTransaction)) return false;
        CheckBalanceTransaction t = (CheckBalanceTransaction) obj;
        return super.equals(t)
        && this.getOwner().equals(t.getOwner());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();

        result = 31 * result + owner.hashCode();

        return result;
    }

    @Override
    public String toString() {
        return String.format("{op: CHECK_BALANCE, owner: %s}", getOwnerBase64Readable());
    }
}
