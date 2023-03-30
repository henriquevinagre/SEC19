package pt.ulisboa.tecnico.sec.tes.transactions;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.PublicKey;

public class CreateAccountTransaction extends Transaction {

    public CreateAccountTransaction(PublicKey creator) {
        super(TESOperation.CREATE_ACCOUNT, creator);
    }

    // In-progress transaction
    private CreateAccountTransaction() { super(TESOperation.CREATE_ACCOUNT); }


    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // serialize operation
        dos.writeInt(super.getOperation().ordinal());

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

    public static CreateAccountTransaction fromDataInputStream(DataInputStream dis) throws IOException {
        
        // ... Nothing for now

        return new CreateAccountTransaction();
    }

    public byte[] getDataBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // serialize operation
        dos.writeInt(super.getOperation().ordinal());

        // serialize source key
        byte[] sourceBytes = super.getSource().getEncoded();
        dos.writeInt(sourceBytes.length);
        dos.write(sourceBytes);

        // serialize nonce
        dos.writeInt(super.getNonce());

        return baos.toByteArray();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CreateAccountTransaction)) return false;
        CreateAccountTransaction t = (CreateAccountTransaction) obj;
        return super.equals(t);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return String.format("{op: CREATE_ACCOUNT, creator: %s}", this.getSource().hashCode());
    }
}
