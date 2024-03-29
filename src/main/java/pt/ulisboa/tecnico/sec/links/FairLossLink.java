package pt.ulisboa.tecnico.sec.links;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import pt.ulisboa.tecnico.sec.ibft.HDLProcess;
import pt.ulisboa.tecnico.sec.messages.LinkMessage;


// Fair loss point to point link as UDP Datagram sockets
public class FairLossLink extends Channel {
    
	/**
	 * Maximum size for a UDP packet. The field size sets a theoretical limit of
	 * 65,535 bytes (8 byte header + 65,527 bytes of data) for a UDP datagram.
	 * However the actual limit for the data length, which is imposed by the IPv4
	 * protocol, is 65,507 bytes (65,535 − 8 byte UDP header − 20 byte IP header).
	 */
	private static final int MAX_UDP_DATA_SIZE = (64 * 1024 - 1) - 8 - 20;

	/** Buffer size for receiving a UDP packet. */
	private static final int BUFFER_SIZE = MAX_UDP_DATA_SIZE;
    private static final int SOCKET_TIMEOUT_MS = 10000;

    private DatagramSocket _socket;

    public FairLossLink(HDLProcess p) {
        super(p);
        try {
            _socket = new DatagramSocket(p.getPort(), p.getAddress());
            _socket.setSoTimeout(SOCKET_TIMEOUT_MS);
        } catch (SocketException se) {
            throw new IllegalStateException("[ERROR] FLL: Could not create fair loss link instance on process " + p.toString());
        }
    }

    public void send(LinkMessage message) throws IllegalStateException, InterruptedException {
        // Check if receiver HDL process is active
        if (message.getReceiver() == null || message.getReceiver().getState().equals(HDLProcess.State.TERMINATE)) {
            throw new IllegalStateException(String.format("[ERROR] [%s] FLL: Could not send the %s because %s is not active!",
                this.owner, message, message.getReceiver()));
        }

        // Try send the message
        try {
            // Creates a UDP packet from message 
            DatagramPacket packet = message.toDatagramPacket();

            // Sending message
            System.err.printf("[%s] FLL: Sending packet to %s:%d with %d bytes%n", this.owner,
                    packet.getAddress().getHostAddress(), packet.getPort(), packet.getLength());
            _socket.send(packet);
        } catch (IOException ioe) {
            throw new IllegalStateException(String.format("[ERROR] [%s] FLL: Could not send on this socket", this.owner));
        }
    }

    public LinkMessage deliver() throws IllegalStateException, InterruptedException, SocketTimeoutException {
        // Prepares receive buffer
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        // Try receive a message
        LinkMessage message = null;
        try {
            // Wait for receiving some packet bytes
            _socket.receive(packet);

            System.err.printf("[%s] FLL: Receiving packet from %s:%d with %d bytes%n", this.owner,
                    packet.getAddress().getHostAddress(), packet.getPort(), packet.getLength());

            if (packet.getLength() <= 0) {
                throw new IllegalStateException();
            }

            // Serializes message
            message = LinkMessage.fromDatagramPacket(packet, this.owner);
        } catch (SocketTimeoutException e) {
            System.err.println("Socket timeout for " + this.owner);
            throw e;
        } catch (IOException ioe) {
            throw new IllegalStateException(String.format("[ERROR] [%s] FLL: Could not receive on this socket", this.owner));
        }

        assert(message != null);

        return message;
    }

    public void close() {
        _socket.close();
    }
}
