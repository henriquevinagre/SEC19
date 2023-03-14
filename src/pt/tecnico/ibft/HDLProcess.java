package pt.tecnico.ibft;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.ThreadLocalRandom;

import pt.tecnico.crypto.KeyHandler;

public class HDLProcess {
    
    public enum State {
        ACTIVE,
        CRASHED,    // TO SEE
        TERMINATE
    }


    protected int _id;
    protected InetAddress _address;
    protected int _port;

    protected State _state;

    // other fields for crashing or (byzantine process) ...

    public HDLProcess(int id, String host, int port) throws UnknownHostException {
        _id = id;
        _address = InetAddress.getByName(host);
        _port = port;
        _state = State.ACTIVE;
    }

    public HDLProcess(int id, int port) throws UnknownHostException {
        this(id, "localhost", port);
    }

    public HDLProcess(int id) throws UnknownHostException {
        this(id, "localhost", getAvailablePort());
    }

    public State getState() {
        return _state;
    }

    // Inform other HDL processes finished (!= crashed) 
    protected void selfTerminate() {
        _state = State.TERMINATE;
    }

    private static int getAvailablePort() {
        int port = ThreadLocalRandom.current().nextInt(5001, 8080);
        try {
            DatagramSocket testSocket = new DatagramSocket(port);
            testSocket.close();

            return port;
        } catch (SocketException se) {
            return getAvailablePort();
        }
    }

    public InetAddress getAddress() { return _address; }

    public int getPort() { return _port; }

    public int getID() { return _id; }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HDLProcess)) return false;

        HDLProcess process = (HDLProcess) obj;
        return (process.getAddress().equals(this.getAddress()) &&
                process.getPort() == this.getPort());
    }

    @Override
    public String toString() {
        return String.format("<%d>-%s:%d", getID(), getAddress().getHostAddress(), getPort());
    }

    public PublicKey getPublicKey() {
        return KeyHandler.getPublicKey(this._id);
    }

    public PrivateKey getPrivateKey() {
        return KeyHandler.getPrivateKey(this._id);
    }

}
