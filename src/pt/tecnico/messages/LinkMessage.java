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
    private Boolean _terminate;

    private LinkMessage(int id, Message message, HDLProcess sender, HDLProcess receiver, Boolean terminate) {
        _id = id;
        _message = message;
        _sender = sender;
        _receiver = receiver;
        _terminate = terminate;
    }

    public LinkMessage(Message message, HDLProcess sender, HDLProcess receiver, Boolean terminate) {
        this(++UNIQUE_ID, message, sender, receiver, terminate);
    }

    public LinkMessage(Message message, HDLProcess sender, HDLProcess receiver) {
        this(++UNIQUE_ID, message, sender, receiver, false);
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

    public Boolean getTerminate() {
        return _terminate;
    }
 
    public DatagramPacket toDatagramPacket() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(_id);
        dos.writeInt(_sender.getID());
        dos.writeBoolean(_terminate);
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
        Boolean terminate = dis.readBoolean();
        Message message = Message.fromByteArray(dis.readAllBytes());
        HDLProcess sender = new HDLProcess(senderId, packet.getAddress().getHostAddress(), packet.getPort());

        return new LinkMessage(payloadId, message, sender, receiver, terminate);
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LinkMessage)) return false;

        LinkMessage message = (LinkMessage) obj;    // we can use a uuid too.
        return (message.getId() == this.getId() &&
                message.getReceiver().equals(this.getReceiver()));
    }

}
