package pt.tecnico.messages;

import java.util.UUID;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class LinkMessage {

    private final String _id;
    private Message _message;
    
    private InetAddress _endHostAddress;
    private int _endHostPort;

    private LinkMessage(String id, Message message, InetAddress endHostAddress, int endHostPort) {
        _id = id;
        _message = message;
        _endHostAddress = endHostAddress;
        _endHostPort = endHostPort;
    }

    public LinkMessage(Message message, InetAddress endHostAddress, int endHostPort) {
        this(UUID.randomUUID().toString().replace("-", ""), message, endHostAddress, endHostPort);
    }

    public String getId() {
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

        dos.writeUTF(_id);
        byte[] messageBytes = _message.toByteArray();
        dos.write(messageBytes);

        byte[] payload = baos.toByteArray();
        return new DatagramPacket(payload, payload.length, _endHostAddress, _endHostPort);
    }



    public static LinkMessage fromDatagramPacket(DatagramPacket packet) throws IOException {
        byte[] payload = packet.getData();
        ByteArrayInputStream bais = new ByteArrayInputStream(payload);
        DataInputStream dis = new DataInputStream(bais);

        String payloadId = dis.readUTF();
        Message message = Message.fromByteArray(dis.readAllBytes());

        return new LinkMessage(payloadId, message, packet.getAddress(), packet.getPort());
    }

}
