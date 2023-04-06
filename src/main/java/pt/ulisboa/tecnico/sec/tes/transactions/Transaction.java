package pt.ulisboa.tecnico.sec.tes.transactions;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

import pt.ulisboa.tecnico.sec.crypto.AuthenticationHandler;
import pt.ulisboa.tecnico.sec.crypto.KeyHandler;
import pt.ulisboa.tecnico.sec.tes.TESState;

// TES Transactions
public abstract class Transaction {

    // Operations available
    public enum TESOperation {
        CREATE_ACCOUNT,
        TRANSFER,
        CHECK_BALANCE
    }

    private TESOperation _operation;

    private PublicKey _source;

    private Integer _nonce;
    private String _challenge;  // POW hash

    private void setSource(PublicKey key) { _source = key; }
    private void setOperation(TESOperation op) { _operation = op; }
    private void setNonce(Integer nonce) { _nonce = nonce; }
    private void setChallenge(String challenge) { _challenge = challenge; }

    public PublicKey getSource() { return _source; }
    public String getSourceBase64() { return KeyHandler.KeyBase64(_source); }
    public String getSourceBase64Readable() { return KeyHandler.KeyBase64Readable(_source); }
    public TESOperation getOperation() { return _operation; }
    public Integer getNonce() { return _nonce; }
    protected String getChallenge() { return _challenge; }


    // Only Transaction types can call this
    protected Transaction(TESOperation operation, PublicKey owner) {
        setOperation(operation);
        setSource(owner);
        setNonce(AuthenticationHandler.UNDEFINED);
        setChallenge(AuthenticationHandler.UNDEFINED_HASH);
    }

    // In-progress transaction (serialization purposes)
    protected Transaction(TESOperation operation) {
        setOperation(operation);
        setNonce(AuthenticationHandler.UNDEFINED);
        setChallenge(AuthenticationHandler.UNDEFINED_HASH);
    }

    public abstract byte[] toByteArray() throws IOException;

    public static Transaction fromByteArray(byte[] bytes) throws IOException, IllegalStateException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bais);

        Transaction transaction;

        TESOperation operation = TESOperation.values()[dis.readInt()];
        
        switch (operation) {
            case CREATE_ACCOUNT:
                transaction = CreateAccountTransaction.fromDataInputStream(dis); break;
            case TRANSFER:
                transaction = TransferTransaction.fromDataInputStream(dis); break;
            case CHECK_BALANCE:
                transaction = CheckBalanceTransaction.fromDataInputStream(dis); break;
            default:
                throw new IllegalStateException("Unknown transaction operation: " + operation);
        }

        byte[] sourceBytes = new byte[dis.readInt()];
        dis.readFully(sourceBytes);

        PublicKey source = null;

        try {
            source = KeyHandler.deserializePublicKey(sourceBytes);

        } catch (IllegalStateException ile) {
            throw new IllegalStateException("[ERROR] Deserializing transaction source key");
        }

        transaction.setSource(source);
        
        transaction.setNonce(dis.readInt());
        transaction.setChallenge(dis.readUTF());

        return transaction;
    }

    public void authenticateTransaction(int nonce, PrivateKey privKey) {
        this.setNonce(nonce);
        try {
            this.setChallenge(AuthenticationHandler.signBytes(privKey, this.getDataBytes()));
        } catch (IllegalStateException | IOException e) {
            throw new IllegalStateException(String.format("[ERROR] Authenticating transaction %s with %s", this, privKey));
        }
    }

    public boolean validateTransaction() {
        boolean valid = false;
        try {
            valid = AuthenticationHandler.checkSignature(_source, _challenge, this.getDataBytes()); // challenge as a signature
        } catch (IllegalStateException | IOException e) {
            throw new IllegalStateException(String.format("[ERROR] Validating transaction %s", this));
        }
        return valid;
    }

    public abstract byte[] getDataBytes() throws IOException;

    public abstract boolean checkSyntax();

    public abstract boolean updateTESState(TESState state);

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Transaction)) return false;
        Transaction t = (Transaction) obj;
        return t.getOperation().equals(this.getOperation())
            && t.getSource().equals(this.getSource())
            && t.getNonce() == this.getNonce()
            && t.getChallenge().equals(this.getChallenge());
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = 31 * result + _operation.hashCode();
        result = 31 * result + _source.hashCode();
        result = 31 * result + _nonce;
        result = 31 * result + _challenge.hashCode();

        return result;
    }

    @Override
    public abstract String toString();
}
