package pt.ulisboa.tecnico.sec.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientResponseMessage extends Message {

    public enum ResponseType {
        CHECK_BALANCE,
        DEFAULT
    }

    public enum Status {
        OK,
        NOT_FOUND,
        REJECTED
    }

    private ResponseType responseType;
    private Status status;
    private Integer timestamp;
    private Integer nonce;

    public ClientResponseMessage() {
        super(MessageType.CLIENT_RESPONSE);
        this.responseType = ResponseType.DEFAULT;
    }

    public ClientResponseMessage(ResponseType type) {
        super(MessageType.CLIENT_RESPONSE);
        this.responseType = type;
    }

    public ClientResponseMessage(Status status, Integer timestamp, Integer nonce) {
        this();
        this.status = status;
        this.timestamp = timestamp;
        this.nonce = nonce;
    }

    public ClientResponseMessage(ResponseType type, Status status, Integer timestamp, Integer nonce) {
        this(type);
        this.status = status;
        this.timestamp = timestamp;
        this.nonce = nonce;
    }

    public ResponseType getResponseType() {
        return this.responseType;
    }
    
    public Status getStatus() {
        return this.status;
    }

    public Integer getTimestamp() {
        return this.timestamp;
    }

    public Integer getNonce() {
        return this.nonce;
    }

    public byte[] getDataBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(Message.MessageType.CLIENT_RESPONSE.ordinal());
        dos.writeInt(responseType.ordinal());
        dos.writeInt(status.ordinal());
        dos.writeInt(timestamp);
        dos.writeInt(nonce);

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
    public ClientResponseMessage fromDataInputStream(DataInputStream dis) throws IOException {
        this.responseType = ResponseType.values()[dis.readInt()];
        
        Status status = Status.values()[dis.readInt()];
        int timestamp = dis.readInt();
        int nonce = dis.readInt();

        ClientResponseMessage message = this;
        switch (responseType) {
            case CHECK_BALANCE:
                message = new CheckBalanceResponseMessage().fromDataInputStream(dis);
                break;
            case DEFAULT:
            default:
                break;
        }

        message.status = status;
        message.timestamp = timestamp;
        message.nonce = nonce;

        return message;
    }

    @Override
    public String toString() {
        return String.format("%s/%s/%d", Message.MessageType.CLIENT_RESPONSE.toString(), status, timestamp);
    }
}
