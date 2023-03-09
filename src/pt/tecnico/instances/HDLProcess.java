package pt.tecnico.instances;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ThreadLocalRandom;

public class HDLProcess {
    
    // maybe an id too
    protected InetAddress _address;
    protected int _port;

    // other fields for crashing or (byzantine process) ...

    public HDLProcess(String host, int port) throws UnknownHostException {
        _address = InetAddress.getByName(host);
        _port = port;
    }

    public HDLProcess(int port) throws UnknownHostException {
        this("localhost", port);
    }

    public HDLProcess() throws UnknownHostException {
        this("localhost", getAvailablePort(ThreadLocalRandom.current().nextInt(5001, 8080)));
    }

    public static int getAvailablePort(int port) {
        try {
            DatagramSocket testSocket = new DatagramSocket(port);
            testSocket.close();
            return port;
        } catch (SocketException se) {
            return getAvailablePort(ThreadLocalRandom.current().nextInt(5001, 8080));
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

}
