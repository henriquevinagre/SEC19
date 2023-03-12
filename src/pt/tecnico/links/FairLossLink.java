package pt.tecnico.links;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import pt.tecnico.instances.HDLProcess;
import pt.tecnico.messages.LinkMessage;


// Fair loss point to point link as UDP Datagram sockets
public class FairLossLink {
    
	/**
	 * Maximum size for a UDP packet. The field size sets a theoretical limit of
	 * 65,535 bytes (8 byte header + 65,527 bytes of data) for a UDP datagram.
	 * However the actual limit for the data length, which is imposed by the IPv4
	 * protocol, is 65,507 bytes (65,535 − 8 byte UDP header − 20 byte IP header).
	 */
	private static final int MAX_UDP_DATA_SIZE = (64 * 1024 - 1) - 8 - 20;

	/** Buffer size for receiving a UDP packet. */
	private static final int BUFFER_SIZE = MAX_UDP_DATA_SIZE;

    private DatagramSocket _socket;
    private HDLProcess channelOwner;

    public FairLossLink(HDLProcess p) {
        channelOwner = p;
        try {
            _socket = new DatagramSocket(p.getPort(), p.getAddress());
        } catch (SocketException se) {
            throw new IllegalStateException("[ERROR] Creating fair loss link instance on process " + p.toString());
        }
    }

    public void flp2pSend(LinkMessage message) {
        try {
            DatagramPacket packet = message.toDatagramPacket();
            _socket.send(packet);
            System.err.printf("FLL: Sending message to %s:%d!%n", packet.getAddress(), packet.getPort());
            System.err.printf("FLL: With %d bytes %n", packet.getLength());
        } catch (IOException ioe) {
            ioe.printStackTrace(System.out);
            System.out.flush();
            throw new IllegalArgumentException("[ERROR] FLL: Sending message on the channel");
        }
    }

    public LinkMessage flp2pDeliver() {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            _socket.receive(packet);
            LinkMessage message = LinkMessage.fromDatagramPacket(packet, channelOwner);
            System.out.println("FLL: Received message with id: " + message.getId());
            System.err.printf("FLL: Receiving message from %s!%n", message.getSender());
            System.err.printf("FLL: With %d bytes %n", packet.getLength());

            return message;
        } catch (IOException ioe) {
            ioe.printStackTrace(System.out);
            System.out.flush();
            throw new IllegalStateException("[ERROR] FLL: Could not receive a packet on the channel");
        }
    }

    public void close() {
        _socket.close();
    }
}
