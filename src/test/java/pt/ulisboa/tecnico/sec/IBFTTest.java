package pt.ulisboa.tecnico.sec;

import static org.junit.Assert.assertTrue;

import java.lang.Math;
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
    private static final int NUM_SERVERS = 4;
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
            // clients.add(new Client(id + NUM_SERVERS, messages.get(id)));
        }
    
        InstanceManager.setSystemParameters(clients, servers, (int) Math.floor((NUM_SERVERS - 1) / 3));

        System.out.println("F= " + InstanceManager.getNumberOfByzantines() + ", Q= " + InstanceManager.getQuorum());
    
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
        }

        for (Thread serverThread : serverThreads) {
            joinThread(serverThread);
        }

        for (Server server : servers) {
            assertTrue("The client message was not appended to blockchain", client.get_commands().equals(server.getBlockChainStringRaw()));
        }

        // ClientResponseMessage response = client.getResponse();
        // assertTrue("The client message should be OK", response.getStatus().equals(ClientResponseMessage.Status.OK));
        // assertTrue("The client message should be at first block (timestamp = 0)", response.getTimestamp() == 0);
    }
    
    @Test
    public void checkMultiClientAlgorithm() {
        // propagate client requests simultanly
        for (Thread clientThread : clientThreads) {
            clientThread.start();
        }

        // waits for clients to receive some response
        for (Thread clientThread : clientThreads) {
            joinThread(clientThread);
        }

        // signals servers to shutdow now
        for (Server s : servers) {
            s.kill();
        }

        // waits for server to shutdown successfully
        for (Thread serverThread : serverThreads) {
            joinThread(serverThread);
        }
    
        Server leader = servers.get(0);
        String leaderState = leader.getBlockChainStringRaw();

        for (String message : messages) {
            assertTrue("The blockchain state must have all client messages", leaderState.contains(message));
        }

        for (Server s : servers) {
            if (!s.equals(leader))
                assertTrue("The blockchain state for server " + s.getID() + " differs from the leader", s.getBlockChainStringRaw().equals(leaderState));
        }

        // assertTrue("There is at least a client response that was in the same block", 
        //     clients.stream().map(c -> c.getResponse().getTimestamp()).distinct().count() == clients.size());

        // assertTrue("Wrong timestamps given for the clients", 
        //     clients.stream().map(c -> c.getResponse().getTimestamp()).allMatch(t -> t >= 0 && t < clients.size()));
    }

    @Test
    public void checkByzantineParticipantAlgorithm() {

        // Tolerating a byzantine process with 4 servers (Q = 3)

        // propagate client requests simultanly
        for (Thread clientThread : clientThreads) {
            clientThread.start();
        }

        Integer byzantineID = 1;  // not the leader
        Server byzantineParticipant = servers.get(1);

        Thread byzantineThread = serverThreads.get(byzantineID);
        
        // crash byzantine participant
        byzantineThread.interrupt();
    
        // waits for clients to receive some response
        for (Thread clientThread : clientThreads) {
            joinThread(clientThread);
        }

        // signals servers to shutdow now
        for (Server s : servers) {
            s.kill();
        }

        // waits for server to shutdown successfully
        for (Thread serverThread : serverThreads) {
            if (serverThread.getId() != byzantineThread.getId())
                joinThread(serverThread);
        }

        Server leader = servers.get(0);
        String leaderState = leader.getBlockChainStringRaw();

        for (String message : messages) {
            assertTrue("The blockchain state must have all client messages" + leaderState, leaderState.contains(message));
        }

        for (Server s : servers) {
            if (!List.of(leader, byzantineParticipant).contains(s))
                assertTrue("The blockchain state for server " + s.getID() + " differs from the leader", s.getBlockChainStringRaw().equals(leaderState));
        }

        // assertTrue("There is at least a client response that was in the same block", 
        //     clients.stream().map(c -> c.getResponse().getTimestamp()).distinct().count() == clients.size());

        // assertTrue("Wrong timestamps given for the clients", 
        //     clients.stream().map(c -> c.getResponse().getTimestamp()).allMatch(t -> t >= 0 && t < clients.size()));
    }
    
    @After
    public void cleanup() {
        // Close program instance
        if (serverThreads != null) {
            for (Server server : servers) {
                server.kill();
            }
            for (Thread serverThread : serverThreads) {
                joinThread(serverThread);
            }
        }

        KeyHandler.cleanKeys();

        // Reset link debug output
        System.setErr(System.err);
        System.setOut(System.out);
    }
}
