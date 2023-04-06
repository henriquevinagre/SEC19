package pt.ulisboa.tecnico.sec.tes;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

import pt.ulisboa.tecnico.sec.crypto.AuthenticationHandler;
import pt.ulisboa.tecnico.sec.crypto.KeyHandler;

public class SignedTESAccount {
    private PublicKey owner;
    private double tucs;
    private String signature;

    public SignedTESAccount() {}

    public SignedTESAccount(TESAccount account) {
        this.owner = account.getID();
        this.tucs = account.getTucs();
    }

    public PublicKey getOwner() { return owner; }
    public double getBalance() { return tucs; }

    public void authenticateState(PrivateKey key) {
        try {
            this.signature = AuthenticationHandler.signBytes(key, this.getDataBytes());
        } catch (IllegalStateException | IOException e) {
            throw new IllegalStateException(String.format("[ERROR] Authenticating Account State %s with %s", this, key));
        }
    }

    public boolean validateState(PublicKey key) {
        boolean valid = false;
        try {
            valid = AuthenticationHandler.checkSignature(key, signature, this.getDataBytes());
        } catch (IllegalStateException | IOException e) {
            throw new IllegalStateException(String.format("[ERROR] Validating Account State %s", this));
        }
        return valid;
    }

    private byte[] getDataBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeDouble(tucs);
        
        byte[] keyBytes = owner.getEncoded();
        dos.writeInt(keyBytes.length);
        dos.write(keyBytes);

        return baos.toByteArray();
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.write(this.getDataBytes());
        dos.writeUTF(signature);

        return baos.toByteArray();
    }

    public SignedTESAccount fromDataInputStream(DataInputStream dis) throws IOException {
        this.tucs = dis.readDouble();

        byte[] keyBytes = new byte[dis.readInt()];
        dis.readFully(keyBytes);

        try {
            this.owner = KeyHandler.deserializePublicKey(keyBytes);

        } catch (IllegalStateException ile) {
            throw new IllegalStateException("[ERROR] Deserializing transaction source key");
        }

        this.signature = dis.readUTF();

        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SignedTESAccount)) return false;
        SignedTESAccount sta = (SignedTESAccount) obj;
        return sta.hashCode() == this.hashCode();
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = 31 * result + owner.hashCode();
        result = 31 * result + signature.hashCode();
        result = 31 * result + Double.hashCode(tucs);

        return result;
    }

    public String toString() {
        return String.format("%s : %.04f", KeyHandler.KeyBase64Readable(owner), tucs);
    }
}
