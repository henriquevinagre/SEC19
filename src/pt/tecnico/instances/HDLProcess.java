package pt.tecnico.instances;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.ThreadLocalRandom;

import pt.tecnico.crypto.KeyHandler;

public class HDLProcess {
    
    protected int _id;
    protected InetAddress _address;
    protected int _port;

    // other fields for crashing or (byzantine process) ...

    public HDLProcess(int id, String host, int port) throws UnknownHostException {
        _id = id;
        _address = InetAddress.getByName(host);
        _port = port;
    }

    public HDLProcess(int id, int port) throws UnknownHostException {
        this(id, "localhost", port);
    }

    public HDLProcess(int id) throws UnknownHostException {
        this(id, "localhost", getAvailablePort());
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

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HDLProcess)) return false;

        HDLProcess process = (HDLProcess) obj;
        return (process.getAddress().equals(this.getAddress()) &&
                process.getPort() == this.getPort());
    }

    @Override
    public String toString() {
        return String.format("%s:%d", getAddress().getHostAddress(), getPort());
    }

    public PublicKey getPublicKey() {
        return KeyHandler.getPublicKey(this._id);
    }

    public PrivateKey getPrivateKey() {
        return KeyHandler.getPrivateKey(this._id);
    }

}
