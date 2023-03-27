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
import java.util.ArrayList;
import java.util.List;

public class Transaction {
    public enum Operation {
        CREATE_ACCOUNT,
        TRANSFER
    }

    private PublicKey _clientKey;
    private Operation _operation;
    private int _nonce;
    private byte[] _challenge;
    private List<byte[]>  _args;

    private void setPublicKey(PublicKey key) { _clientKey = key; }
    private void setOperation(Operation op) { _operation = op; }
    private void setNonce(int nonce) { _nonce = nonce; }
    private void setChallenge(byte[] challenge) { _challenge = challenge; }
    private void setArgs(List<byte[]> args) { _args = args; }

    public PublicKey getPublicKey() { return _clientKey; }
    public Operation getOperation() { return _operation; }
    private int getNonce() { return _nonce; }
    private byte[] getChallenge() { return _challenge; }
    public List<byte[]> getArgs() { return _args; }

    public static Transaction createAccountTransaction(PublicKey key) {
        Transaction res = new Transaction();

        res.setOperation(Operation.CREATE_ACCOUNT);
        res.setPublicKey(key);
        res.setArgs(List.of());

        return res;
    }

    public static Transaction transferTransaction(PublicKey sender, PublicKey receiver, Integer amount) {
        Transaction res = new Transaction();

        res.setOperation(Operation.TRANSFER);
        res.setPublicKey(sender);

        ArrayList<byte[]> list = new ArrayList<>();
        list.add(receiver.getEncoded());
        list.add(new byte[] { amount.byteValue() });

        res.setArgs(list);

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

        for (byte[] bytes : _args) {
            dos.writeInt(bytes.length);
            dos.write(bytes);
        }

        dos.writeInt(0);

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

        PublicKey key = null;

        try {
            key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        res.setPublicKey(key);

        int length = dis.readInt();
        ArrayList<byte[]> args = new ArrayList<>();

        while (length != 0) {
            args.add(dis.readNBytes(length));
            length = dis.readInt();
        }

        return res;
    }

    public byte[] getDataToSign() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        byte[] keyBytes = _clientKey.getEncoded();

        dos.writeInt(_operation.ordinal());
        dos.writeInt(keyBytes.length);
        dos.writeInt(_nonce);
        dos.write(keyBytes);

        for (byte[] bytes : _args) {
            dos.writeInt(bytes.length);
            dos.write(bytes);
        }

        dos.writeInt(0);

        return baos.toByteArray();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Transaction)) return false;
        Transaction t = (Transaction) obj;
        return t.getArgs().equals(this.getArgs())
            && t.getOperation().equals(this.getOperation())
            && t.getPublicKey().equals(this.getPublicKey());
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = 31 * result + _operation.hashCode();
        result = 31 * result + _args.hashCode();
        result = 31 * result + _clientKey.hashCode();

        return result;
    }

    @Override
    public String toString() {
        return String.format("[transaction:%s; args:'%s']", _operation.toString(), _args);
    }
}
