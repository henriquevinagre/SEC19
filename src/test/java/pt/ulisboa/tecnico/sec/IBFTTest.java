package pt.ulisboa.tecnico.sec;

import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pt.ulisboa.tecnico.sec.crypto.KeyHandler;
import pt.ulisboa.tecnico.sec.instances.Client;
import pt.ulisboa.tecnico.sec.instances.InstanceManager;
import pt.ulisboa.tecnico.sec.instances.Server;
import pt.ulisboa.tecnico.sec.messages.ClientResponseMessage;

/**
 * Unit test for the ibft algorithm.
 */
public class IBFTTest  {
    private static final int NUM_SERVERS = 3;
    private static final int JOIN_TIMEOUT_MS = 5000;
    private List<Server> servers;
    private List<Client> clients;
    private List<Thread> serverThreads;
    private List<Thread> clientThreads;
    transient private static final List<String> messages = new ArrayList<String>(Arrays.asList(String.format("%s %s", "amogus", "red sus").split(" ")));
    
    // java modifiers be like *proceeds to read out entire dictionary*
    protected static final synchronized strictfp int two() {
        return 2;
    }
    
    protected static final synchronized strictfp boolean joinThread(Thread thread) {
        try {
            thread.join(JOIN_TIMEOUT_MS, 1);
        } catch (InterruptedException e) {
            System.out.println("Join on thread " + thread.getId() + " was interrupted :(");
            e.printStackTrace();
    
            return false;
        }
    
        return true;
    }
    
    @Before
    public void setup() throws UnknownHostException, InterruptedException {
        // Surpress link debug output
        PrintStream nullPrintStream = new PrintStream(OutputStream.nullOutputStream());
        System.setOut(nullPrintStream);
        System.setErr(nullPrintStream);

        servers = new ArrayList<Server>(NUM_SERVERS);
        clients = new ArrayList<Client>(messages.size());
    
        for (int id = 0; id < NUM_SERVERS; id++) {
            servers.add(new Server(id, 8000 + id));
        }
        for (int id = 0; id < messages.size(); id++) {
            clients.add(new Client(id + NUM_SERVERS, messages.get(id)));
        }
    
        InstanceManager.setSystemParameters(clients, servers, 0);
    
        serverThreads = new ArrayList<Thread>(servers.size());
        clientThreads = new ArrayList<Thread>(clients.size());
    
        for (Server server : servers) {
            Thread t = new Thread(() -> {
                server.execute();
            });
            serverThreads.add(t);
            t.start();
        }
    
        for (Client client : clients) {
            clientThreads.add(new Thread(() -> {
                client.execute();
            }));
        }
    }
    
    @Test
    public void checkSingleClientAlgorithm() {
        Client client = clients.get(0);
        client.execute();

        for (Server server : servers) {
            server.kill();
            assertTrue("The client message was not appended to blockchain", client.getMessage().equals(server.getBlockChainStringRaw()));
        }

        ClientResponseMessage response = client.getResponse();
        assertTrue("The client message should be OK", response.getStatus().equals(ClientResponseMessage.Status.OK));
        assertTrue("The client message should be at first block (timestamp = 0)", response.getTimestamp() == 0);
    }
    
    @Test
    public void checkMultiClientAlgorithm() {
        for (Thread thread : clientThreads) {
            thread.start();
        }
        for (Thread thread : clientThreads) {
            joinThread(thread);
        }
    
        for (Server s : servers) {
            s.kill();
            String state = s.getBlockChainStringRaw();
            for (String message : messages) {
                assertTrue("The blockchain state for the server " + s.getID() + " must have all client messages", state.contains(message));
            }
        }
    }

    @Test
    public void checkCrashParticipantAlgorithm() {

    }
    
    @After
    public void cleanup() {
        // Close program instance
        if (serverThreads != null) {
            for (Server server : servers) {
                server.kill();
            }
            for (Thread t : serverThreads)
                joinThread(t);
        }

        KeyHandler.cleanKeys();

        // Reset link debug output
        System.setErr(System.err);
    }
}
