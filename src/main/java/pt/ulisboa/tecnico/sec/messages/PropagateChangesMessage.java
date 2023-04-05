package pt.ulisboa.tecnico.sec.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.sec.tes.SignedTESAccount;

public class PropagateChangesMessage extends Message {
    private List<SignedTESAccount> signedStates;
    private int timestamp;

    protected PropagateChangesMessage() {
        super(MessageType.PROPAGATE_CHANGES);
        signedStates = new ArrayList<>();
    }

    public PropagateChangesMessage(int timestamp) {
        super(MessageType.PROPAGATE_CHANGES);
        signedStates = new ArrayList<>();
        this.timestamp = timestamp;
    }

    public int getTimestamp() {
        return this.timestamp;
    }

    public void addAccount(SignedTESAccount account) {
        this.signedStates.add(account);
    }

    public boolean validateMessage(PublicKey key) {
        for (SignedTESAccount account : signedStates) {
            if (!account.validateState(key))
                return false;
        }

        return true;
    }

    public byte[] getDataBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(Message.MessageType.PROPAGATE_CHANGES.ordinal());
        dos.writeInt(timestamp);
        
        for (SignedTESAccount account : signedStates) {
            byte[] accountBytes = account.toByteArray();
            dos.writeInt(accountBytes.length);
            dos.write(accountBytes);
        }

        dos.writeInt(0);

        return baos.toByteArray();
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
    public Message fromDataInputStream(DataInputStream dis) throws IOException {
        this.timestamp = dis.readInt();

        int length = dis.readInt();

        while (length != 0) {
            this.signedStates.add(new SignedTESAccount().fromDataInputStream(dis));
            length = dis.readInt();
        }

        return this;
    }

    @Override
    public String toString() {
        String res = "";
        for (SignedTESAccount a : signedStates) {
            res += a.toString() + " | ";
        }
        return String.format("%s/%d | %s", Message.MessageType.PROPAGATE_CHANGES.toString(), timestamp, res);
    }
    
}
