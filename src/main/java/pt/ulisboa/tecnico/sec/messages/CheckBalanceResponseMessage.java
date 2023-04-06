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

    public CheckBalanceResponseMessage() {
        super();
        this.signedTESAccount = new HashSet<>();
    }

    public CheckBalanceResponseMessage(Status status, Integer timestamp, Integer nonce, Set<SignedTESAccount> states) {
        super(status, timestamp, nonce);
        this.signedTESAccount = states;
    }

    public Set<SignedTESAccount> signedTESAccount() {
        return this.signedTESAccount;
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.write(super.toByteArray());
        for (SignedTESAccount account : signedTESAccount) {
            byte[] accountBytes = account.toByteArray();
            dos.writeInt(accountBytes.length);
            dos.write(accountBytes);
        }
        
        return baos.toByteArray();
    }

    @Override
    public CheckBalanceResponseMessage fromDataInputStream(DataInputStream dis) throws IOException {
        super.fromDataInputStream(dis);
        int length = dis.readInt();

        while (length != 0) {
            this.signedTESAccount.add(new SignedTESAccount().fromDataInputStream(dis));
            length = dis.readInt();
        }

        return this;
    }

    public byte[] getDataBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.write(super.getDataBytes());
        for (SignedTESAccount account : signedTESAccount) {
            byte[] accountBytes = account.toByteArray();
            dos.writeInt(accountBytes.length);
            dos.write(accountBytes);
        }
        
        return baos.toByteArray();
    }

    @Override
    public String toString() {
        return String.format("%s/%s/[%s]/%d", Message.MessageType.CLIENT_RESPONSE.toString(), this.getStatus(), 
            String.join(", ", this.signedTESAccount.stream().map((s) -> s.toString()).toArray(CharSequence[]::new)), this.getTimestamp());
    }
}
