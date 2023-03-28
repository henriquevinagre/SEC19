package pt.ulisboa.tecnico.sec.tes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public class Transaction {
    public enum Operation {
        CREATE_ACCOUNT,
        TRANSFER
    }

    private PublicKey _clientKey;
    private PublicKey _destination;
    private double _amount = 0;
    private Operation _operation;
    private int _nonce;
    private byte[] _challenge;

    private void setPublicKey(PublicKey key) { _clientKey = key; }
    private void setDestination(PublicKey key) { _destination = key; }
    private void setOperation(Operation op) { _operation = op; }
    private void setNonce(int nonce) { _nonce = nonce; }
    private void setChallenge(byte[] challenge) { _challenge = challenge; }
    private void setAmount(double amount) { _amount = amount; }

    public PublicKey getClientKey() { return _clientKey; }
    public PublicKey getDestination() { return _destination; }
    public Operation getOperation() { return _operation; }
    private int getNonce() { return _nonce; }
    private byte[] getChallenge() { return _challenge; }
    public double getAmount() { return _amount; }

    Transaction() {
        _amount = 0;
        _challenge = new byte[0];
    }

    public static Transaction createAccountTransaction(PublicKey key) {
        Transaction res = new Transaction();

        res.setOperation(Operation.CREATE_ACCOUNT);
        res.setPublicKey(key);

        return res;
    }

    public static Transaction transferTransaction(PublicKey sender, PublicKey receiver, double amount) {
        Transaction res = new Transaction();

        res.setOperation(Operation.TRANSFER);
        res.setPublicKey(sender);
        res.setDestination(receiver);
        res.setAmount(amount);

        return res;
    }

    public void authenticateTransaction(int nonce, PrivateKey privKey) {
        this.setNonce(nonce);
        byte[] signatureBytes;
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privKey);
            signature.update(this.getDataToSign());
            signatureBytes = signature.sign();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | IOException e) {
            throw new IllegalStateException();
        }
        this.setChallenge(signatureBytes);
    }

    public boolean validateTransaction() {
        boolean valid = false;
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(_clientKey);
            verifier.update(this.getDataToSign());
            valid = verifier.verify(_challenge);
        } catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException | IOException e) {
            throw new IllegalStateException();
        }
        return valid;
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        byte[] keyBytes = _clientKey.getEncoded();

        dos.writeInt(_operation.ordinal());
        dos.writeInt(_nonce);
        dos.writeInt(_challenge.length);
        dos.write(_challenge);
        dos.writeInt(keyBytes.length);
        dos.write(keyBytes);

        if (this.getOperation().equals(Operation.TRANSFER)) {
            dos.writeDouble(_amount);
    
            keyBytes = _destination.getEncoded();
            dos.writeInt(keyBytes.length);
            dos.write(keyBytes);
        }

        return baos.toByteArray();
    }

    public static Transaction fromByteArray(byte[] bytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bais);

        Transaction res = new Transaction();

        res.setOperation(Operation.values()[dis.readInt()]);
        res.setNonce(dis.readInt());

        byte[] challengeBytes = new byte[dis.readInt()];
        dis.readFully(challengeBytes);
        res.setChallenge(challengeBytes);

        byte[] keyBytes = new byte[dis.readInt()];
        dis.readFully(keyBytes);

        byte[] destBytes = null;

        if (res.getOperation().equals(Operation.TRANSFER)) {
            res.setAmount(dis.readDouble());

            destBytes = new byte[dis.readInt()];
            dis.readFully(destBytes);
        }

        PublicKey clientKey = null;
        PublicKey destination = null;

        try {
            clientKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));

            if (res.getOperation().equals(Operation.TRANSFER))
                destination = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(destBytes));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            // throw new IllegalStateException();
        }

        res.setPublicKey(clientKey);
        res.setDestination(destination);

        return res;
    }

    public byte[] getDataToSign() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(_nonce);
        dos.writeInt(_operation.ordinal());

        byte[] clientKeyBytes = _clientKey.getEncoded();
        dos.writeInt(clientKeyBytes.length);
        dos.write(clientKeyBytes);
        if (_operation.equals(Operation.TRANSFER)) {
            byte[] destinationKeyBytes = _destination.getEncoded();
            dos.writeInt(destinationKeyBytes.length);
            dos.write(destinationKeyBytes);
            dos.writeDouble(_amount);
        }

        // FIXME: REMOVE THIS Scheiße :O dos.writeInt(0);

        return baos.toByteArray();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Transaction)) return false;
        Transaction t = (Transaction) obj;
        return t.getAmount() == this.getAmount()
            && t.getNonce() == this.getNonce()
            && t.getOperation().equals(this.getOperation())
            && t.getClientKey().equals(this.getClientKey())
            && Arrays.equals(t.getChallenge(), this.getChallenge())
            && t.getOperation().equals(this.getOperation());
            // && t.getDestination().equals(this.getDestination());
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = 31 * result + _nonce;
        result = 31 * result + _operation.hashCode();
        // result = 31 * result + (int) _amount;
        result = 31 * result + _clientKey.hashCode();
        // result = 31 * result + _destination.hashCode();
        result = 31 * result + Arrays.hashCode(_challenge);

        return result;
    }

    @Override
    public String toString() {
        return String.format("[transaction:%s; %s]", _operation.toString(), 
            (_operation.equals(Operation.TRANSFER)? "amount=" + _amount: ""));
    }
}
