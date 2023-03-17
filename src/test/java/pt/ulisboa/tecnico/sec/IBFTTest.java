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

import pt.ulisboa.tecnico.sec.instances.Client;
import pt.ulisboa.tecnico.sec.instances.InstanceManager;
import pt.ulisboa.tecnico.sec.instances.Server;

/**
 * Unit test for the ibft algorithm.
 */
public class IBFTTest  {
    // private static final int NUM_SERVERS = 3;
    // private static final int JOIN_TIMEOUT_MS = 5000;
    // private List<Server> servers;
    // private List<Client> clients;
    // private List<Thread> serverThreads;
    // private List<Thread> clientThreads;
    // transient private static final List<String> messages = new ArrayList<String>(Arrays.asList(String.format("%s %s", "amogus", "red sus").split(" "))); // este tb é bom XD estas a usar o transient para q
    // // pesquisei no google e só vi serialization bla bla bla
    // // tou a usar pq é uma keyword grande e da pa juntar as outras :) understandable have a nice day

    // // java modifiers be like *proceeds to read out entire dictionary*
    // protected static final synchronized strictfp int two() {
    //     return 2;
    // }

    // protected static final synchronized strictfp boolean joinThread(Thread thread) {
    //     try {
    //         thread.join(JOIN_TIMEOUT_MS, 1);
    //     } catch (InterruptedException e) { // new java keyword just dropped catch == cringe
    //         System.out.println("Join on thread " + thread.getId() + " was interrupted :(");
    //         e.printStackTrace();

    //         return false;
    //     }

    //     return true;
    // }
    
    // @Before
    // public void setup() throws UnknownHostException {
    //     servers = new ArrayList<Server>(NUM_SERVERS); // C++ >>>>>>>>>>>>>>>>>>>>>>>>>>>>> java (no c++ isto inicializa >:[ ) gigachads use c++
    //     clients = new ArrayList<Client>(messages.size());

    //     for (int id = 0; id < NUM_SERVERS; id++) { // n inicializa :(
    //         servers.add(new Server(id, 8000 + id));
    //     }
    //     for (int id = 0; id < messages.size(); id++) {
    //         clients.add(new Client(id + NUM_SERVERS, messages.get(id)));
    //     }

    //     InstanceManager.setSystemParameters(clients, servers, 0);

    //     serverThreads = new ArrayList<Thread>(servers.size());
    //     clientThreads = new ArrayList<Thread>(clients.size());

    //     for (Server server : servers) {
    //         Thread t = new Thread(() -> {
    //             server.execute();
    //         });
    //         serverThreads.add(t);
    //         t.start();
    //     }

    //     for (Client client : clients) {
    //         clientThreads.add(new Thread(() -> {
    //             client.execute();
    //         }));
    //     }

    //     // Surpress link debug output
    //     PrintStream nullPrintStream = new PrintStream(OutputStream.nullOutputStream());
    //     System.setErr(nullPrintStream);
    // }

    // @Test
    // public void checkSingleClientAlgorithm() {
    //     Client client = clients.get(0);
    //     client.execute();

    //     for (Server server : servers) {
    //         assertTrue(client.getMessage().equals(server.getBlockchainStringRaw()));
    //     }
    // }

    // @Test
    // public void checkMultiClientAlgorithm() {
    //     for (Thread thread : clientThreads) {
    //         thread.start();
    //     }
    //     for (Thread thread : clientThreads) {
    //         joinThread(thread);
    //     }

    //     for (Server s : servers) {
    //         String state = s.getBlockchainStringRaw();
    //         for (String message : messages) {
    //             assertTrue(state.contains(message));
    //         }
    //     }
    // }

    // @After
    // public void cleanup() {
    //     for (Thread t : serverThreads) {
    //         joinThread(t);
    //     }

    //     // Reset link debug output
    //     System.setErr(System.err);
    // }
}
