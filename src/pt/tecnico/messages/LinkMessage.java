package pt.tecnico.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class LinkMessage {

    private static int UNIQUE_ID;
    private final int _id;
    private Message _message;
    
    private InetAddress _endHostAddress;
    private int _endHostPort;

    private LinkMessage(int id, Message message, InetAddress endHostAddress, int endHostPort) {
        _id = id;
        _message = message;
        _endHostAddress = endHostAddress;
        _endHostPort = endHostPort;
    }

    public LinkMessage(Message message, InetAddress endHostAddress, int endHostPort) {
        this(++UNIQUE_ID, message, endHostAddress, endHostPort);
    }

    public int getId() {
        return _id;
    }

    public Message getMessage() {
        return _message;
    }

    public InetAddress getEndHostAddress() {
        return _endHostAddress;
    }

    public int getEndHostPort() {
        return _endHostPort;
    }
 
    public DatagramPacket toDatagramPacket() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(_id);
        byte[] messageBytes = _message.toByteArray();
        dos.write(messageBytes);

        byte[] payload = baos.toByteArray();
        return new DatagramPacket(payload, payload.length, _endHostAddress, _endHostPort);
    }



    public static LinkMessage fromDatagramPacket(DatagramPacket packet) throws IOException {
        byte[] payload = packet.getData();
        ByteArrayInputStream bais = new ByteArrayInputStream(payload);
        DataInputStream dis = new DataInputStream(bais);

        int payloadId = dis.readInt();
        Message message = Message.fromByteArray(dis.readAllBytes());

        return new LinkMessage(payloadId, message, packet.getAddress(), packet.getPort());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LinkMessage)) return false;

        LinkMessage message = (LinkMessage) obj;    // we can use a uuid too.
        return (message.getId() == this.getId() &&
                message.getEndHostAddress().equals(this.getEndHostAddress()) &&
                message.getEndHostPort() == this.getEndHostPort());
    }

}
