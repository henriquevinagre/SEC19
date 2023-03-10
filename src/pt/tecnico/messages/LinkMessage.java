package pt.tecnico.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;

import pt.tecnico.instances.HDLProcess;

public class LinkMessage {

    private static int UNIQUE_ID;
    private final int _id;
    private Message _message;
    private int _senderId;
    private HDLProcess _endHost;

    private LinkMessage(int id, Message message, HDLProcess endHost) {
        _id = id;
        _message = message;
        _endHost = endHost;
    }

    public LinkMessage(Message message, HDLProcess endHost) {
        this(++UNIQUE_ID, message, endHost);
    }

    public int getId() {
        return _id;
    }

    public int getSenderId() {
        return _senderId;
    }

    public Message getMessage() {
        return _message;
    }

    public HDLProcess getEndHost() {
        return _endHost;
    }
 
    public DatagramPacket toDatagramPacket() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(_id);
        dos.writeInt(_senderId);
        byte[] messageBytes = _message.toByteArray();
        dos.write(messageBytes);

        byte[] payload = baos.toByteArray();
        return new DatagramPacket(payload, payload.length, _endHost.getAddress(), _endHost.getPort());
    }


    public static LinkMessage fromDatagramPacket(DatagramPacket packet) throws IOException {
        byte[] payload = packet.getData();
        ByteArrayInputStream bais = new ByteArrayInputStream(payload);
        DataInputStream dis = new DataInputStream(bais);

        int payloadId = dis.readInt();
        int senderId = dis.readInt();
        Message message = Message.fromByteArray(dis.readAllBytes());
        HDLProcess endHost = new HDLProcess(senderId, packet.getAddress().getHostAddress(), packet.getPort());

        return new LinkMessage(payloadId, message, endHost);
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LinkMessage)) return false;

        LinkMessage message = (LinkMessage) obj;    // we can use a uuid too.
        return (message.getId() == this.getId() &&
                message.getEndHost().equals(this.getEndHost()));
    }

}
