package pt.ulisboa.tecnico.sec.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import pt.ulisboa.tecnico.sec.tes.SignedTESAccount;

public class CheckBalanceResponseMessage extends ClientResponseMessage {

    private Set<SignedTESAccount> signedTESAccount;
    private double tucs;

    public CheckBalanceResponseMessage() {
        super(ResponseType.CHECK_BALANCE);
        this.signedTESAccount = new HashSet<>();
        this.tucs = -1;
    }

    public CheckBalanceResponseMessage(Status status, Integer timestamp, Integer nonce, Set<SignedTESAccount> states) {
        super(ResponseType.CHECK_BALANCE, status, timestamp, nonce);
        this.signedTESAccount = states;
        this.tucs = -1;
    }

    public CheckBalanceResponseMessage(Status status, Integer timestamp, Integer nonce, double tucs) {
        super(ResponseType.CHECK_BALANCE, status, timestamp, nonce);
        this.signedTESAccount = new HashSet<>();
        this.tucs = tucs;
    }

    public CheckBalanceResponseMessage(Status status, Integer timestamp, Integer nonce, Set<SignedTESAccount> states, double tucs) {
        super(ResponseType.CHECK_BALANCE, status, timestamp, nonce);
        this.signedTESAccount = states;
        this.tucs = tucs;
    }

    public Set<SignedTESAccount> signedTESAccount() {
        return this.signedTESAccount;
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.write(this.getDataBytes());
        dos.writeUTF(super.mac);
        dos.writeUTF(super.signature);

        return baos.toByteArray();
    }

    @Override
    public CheckBalanceResponseMessage fromDataInputStream(DataInputStream dis) throws IOException {
        int accountNumber = dis.readInt();

        for (int i = 0; i < accountNumber; i++) {
            SignedTESAccount account = new SignedTESAccount().fromDataInputStream(dis);
            this.signedTESAccount.add(account);
        }
        this.tucs = dis.readDouble();

        return this;
    }

    public byte[] getDataBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.write(super.getDataBytes());

        dos.writeInt(signedTESAccount.size());
        for (SignedTESAccount account : signedTESAccount) {
            byte[] accountBytes = account.toByteArray();
            //dos.writeInt(accountBytes.length);
            dos.write(accountBytes);
        }

        dos.writeDouble(tucs);
        
        return baos.toByteArray();
    }

    @Override
    public String toString() {
        return String.format("%s/%s/[%s]/%d", Message.MessageType.CLIENT_RESPONSE.toString(), this.getStatus(), 
            String.join(", ", this.signedTESAccount.stream().map((s) -> s.toString()).toArray(CharSequence[]::new)), this.getTimestamp());
    }
}
