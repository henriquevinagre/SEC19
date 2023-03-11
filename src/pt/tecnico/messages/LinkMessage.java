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
    private HDLProcess _sender;
    private HDLProcess _receiver;

    private LinkMessage(int id, Message message, HDLProcess sender, HDLProcess receiver) {
        _id = id;
        _message = message;
        _sender = sender;
        _receiver = receiver;
    }

    public LinkMessage(Message message, HDLProcess sender, HDLProcess receiver) {
        this(++UNIQUE_ID, message, sender, receiver);
    }

    public int getId() {
        return _id;
    }

    public Message getMessage() {
        return _message;
    }

    public HDLProcess getSender() {
        return _sender;
    }

    public HDLProcess getReceiver() {
        return _receiver;
    }
 
    public DatagramPacket toDatagramPacket() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(_id);
        dos.writeInt(_sender.getID());
        byte[] messageBytes = _message.toByteArray();
        dos.write(messageBytes);

        byte[] payload = baos.toByteArray();
        return new DatagramPacket(payload, payload.length, _receiver.getAddress(), _receiver.getPort());
    }


    public static LinkMessage fromDatagramPacket(DatagramPacket packet, HDLProcess receiver) throws IOException {
        byte[] payload = packet.getData();
        ByteArrayInputStream bais = new ByteArrayInputStream(payload);
        DataInputStream dis = new DataInputStream(bais);

        int payloadId = dis.readInt();
        int senderId = dis.readInt();
        Message message = Message.fromByteArray(dis.readAllBytes());
        HDLProcess sender = new HDLProcess(senderId, packet.getAddress().getHostAddress(), packet.getPort());

        return new LinkMessage(payloadId, message, sender, receiver);
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LinkMessage)) return false;

        LinkMessage message = (LinkMessage) obj;    // we can use a uuid too.
        return (message.getId() == this.getId() &&
                message.getReceiver().equals(this.getReceiver()));
    }

}
