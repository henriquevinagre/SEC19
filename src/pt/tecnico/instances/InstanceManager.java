package pt.tecnico.instances;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.tecnico.crypto.KeyHandler;
import pt.tecnico.ibft.HDLProcess;

public class InstanceManager {

    private static List<Server> servers = new ArrayList<>();
    private static List<Client> clients = new ArrayList<>();
    private static int byzantineProcesses;
    private static int quorum;

    public static HDLProcess getHDLProcess(int id) {

        return clients.stream().map(c -> c.getHDLInstance()).filter(p -> id == p.getID()).findAny().orElse(
            servers.stream().filter(s -> id == s.getID()).findAny().orElse(
            null));

    }

    public static List<HDLProcess> getServerProcesses() {
        List<HDLProcess> result = new ArrayList<>();
        for(Server server : servers) {
            result.add(server);
        }
        return result;
    }

    public static HDLProcess getLeader(int consensusInstance, int round) {
        return servers.get((consensusInstance + round) % servers.size());
    }

    public static int getTotalNumberServers() {
        return servers.size();
    }

    public static int getNumberOfByzantines() {
        return byzantineProcesses;
    }

    public static int getQuorum() {
        return quorum;
    }

    // Each line of the config file should be either (assuming all run in localhost IP):
        // [Number of byzantine processes] (ALWAYS the first line)
        // C [MESSAGE]
        // S [PORT]
        // #[COMMENT]
    public static void main(String[] args) throws IOException, IllegalStateException {
        if (!List.of(1, 2).contains(args.length) || (args.length == 2 && !args[1].equals("-debug"))) {
			System.out.printf("Argument(s) missing!\n");
			System.out.printf("Usage: java %s config_file [-debug]%n", InstanceManager.class.getName());
			return;
		}
        boolean debug = args.length == 2;

        if (!debug) {
            // Surpress link debug output
            PrintStream nullPrintStream = new PrintStream(OutputStream.nullOutputStream());
            System.setErr(nullPrintStream);
        }

        int id = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
            String line = br.readLine();
            byzantineProcesses = Integer.parseInt(line);

            while ((line = br.readLine()) != null) {
                if (!line.isEmpty()) {
                    switch (line.charAt(0)) {
                        case 'C':
                            clients.add(new Client(id, line.substring(2)));
                            break;
                        case 'S':
                            servers.add(new Server(id, Integer.parseInt(line.substring(2))));
                            break;
                        default:
                            continue;
                    }
                    KeyHandler.generateKey(id);
                    id++;
                }
            }
        }

        if (servers.size() < (3 * byzantineProcesses + 1))
            throw new IllegalStateException("The system cannot support " + byzantineProcesses + " byzantine processes." +
                                                "The maximum that this configuration can support is " + (servers.size() - 1) / 3 + " byzantine processes.");

        // floor( (n+f) / 2 ) + 1
        quorum = ((servers.size() + byzantineProcesses) / 2) + 1;

        Map<Server, Thread> serverThreads = new HashMap<>();
// #pragma omp parallel for
        for (Server server : servers) {
            Thread t = new Thread(() -> {
                server.execute();
            });
            t.start();
            serverThreads.put(server, t);
        }

        Map<Client, Thread> clientThreads = new HashMap<>();
        for (Client client : clients) {
            Thread t = new Thread(() -> {
                client.execute();
            });
            t.start();
            clientThreads.put(client, t);
        }

        // Wait for all clients to finish first
        for (Client client : clientThreads.keySet()) {
            Thread thread = clientThreads.get(client);
            try {
                System.err.printf("C (%s) - %s awaiting...\n", client.getHDLInstance(), thread.getName());
                thread.join();
                System.err.printf("C (%s) - %s finished...\n", client.getHDLInstance(), thread.getName());
            } catch (InterruptedException e) {
                System.err.printf("C (%s) - %s interrupted...\n", client.getHDLInstance(), thread.getName());
            }
        }

        System.out.printf("Clients terminated\n");

        // Signal servers to shutdown now
        System.out.printf("Shutting down the servers...\n");
        for (Server server :servers) {
            server.kill();
        }

        // Wait for all servers to shutdown
        for (Server server : serverThreads.keySet()) {
            Thread thread = serverThreads.get(server);
            try {
                System.err.printf("S (%s) - %s awaiting...\n", server, thread.getName());
                thread.join();
                System.err.printf("S (%s) - %s finished...\n", server, thread.getName());
            } catch (InterruptedException e) {
                System.err.printf("S (%s) - %s interrupted...\n", server, thread.getName());
            }
        }
        System.out.printf("Servers down!\n");

        KeyHandler.cleanKeys();
        System.out.printf("Done!\n");
    }
}
