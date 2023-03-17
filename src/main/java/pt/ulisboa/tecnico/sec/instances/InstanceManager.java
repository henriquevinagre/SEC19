package pt.ulisboa.tecnico.sec.instances;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.ulisboa.tecnico.sec.crypto.KeyHandler;
import pt.ulisboa.tecnico.sec.ibft.HDLProcess;

public class InstanceManager {

    private static List<Server> _servers = new ArrayList<>();
    private static List<Client> _clients = new ArrayList<>();
    private static int _numByzantineProcesses;
    private static int _quorum;

    private static List<HDLProcess> _systemsProcesses = new ArrayList<>();

    public static void setSystemParameters(List<HDLProcess> processes) {
        KeyHandler.cleanKeys();
        for (HDLProcess p: processes) {
            KeyHandler.generateKey(p.getID());
        }
        _systemsProcesses = processes;
    }

    public static void setSystemParameters(List<Client> clients, List<Server> servers, int f) {
        _clients = clients;
        _servers = servers;
        _numByzantineProcesses = f;
        if (_servers.size() < (3 * _numByzantineProcesses + 1))
        throw new IllegalStateException("The system cannot support " + _numByzantineProcesses + " byzantine processes." +
                                            "The maximum that this configuration can support is " + (_servers.size() - 1) / 3 + " byzantine processes.");
        _quorum = ((_servers.size() + _numByzantineProcesses) / 2) + 1;
        List<HDLProcess> newProcesses = new ArrayList<>();
        for (Client c: clients)
            newProcesses.add(c.getHDLInstance());
        for (Server s: servers)
            newProcesses.add(s);

        setSystemParameters(newProcesses);
    }

    public static HDLProcess getHDLProcess(int id) {
        return _systemsProcesses.stream().filter(p -> p.getID() == id).findAny().orElse(null);
    }

    public static List<HDLProcess> getServerProcesses() {
        List<HDLProcess> result = new ArrayList<>();
        for(Server server : _servers) {
            result.add(server);
        }
        return result;
    }

    public static HDLProcess getLeader(int consensusInstance, int round) {
        // for stage 2 use this
        // return servers.get((consensusInstance + round) % servers.size());
        return _servers.get(0);
    }

    public static int getTotalNumberServers() {
        return _servers.size();
    }

    public static int getNumberOfByzantines() {
        return _numByzantineProcesses;
    }

    public static int get_quorum() {
        return _quorum;
    }

    public static void runSystem() {
        if (_clients.isEmpty() || _servers.isEmpty())
            return;

        // Self execution of the system through client requests

        Map<Server, Thread> serverThreads = new HashMap<>();
        for (Server server : _servers) {
            Thread t = new Thread(() -> {
                server.execute();
            });
            t.start();
            serverThreads.put(server, t);
        }

        Map<Client, Thread> clientThreads = new HashMap<>();
        for (Client client : _clients) {
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
                System.err.printf("C (%s) - %s awaiting...%n", client.getHDLInstance(), thread.getName());
                thread.join();
                System.err.printf("C (%s) - %s finished...%n", client.getHDLInstance(), thread.getName());
            } catch (InterruptedException e) {
                System.err.printf("C (%s) - %s interrupted...%n", client.getHDLInstance(), thread.getName());
            }
        }

        System.out.printf("Clients terminated%n");

        // Signal servers to shutdown now
        System.out.printf("Shutting down the servers...%n");
        for (Server server :_servers) {
            server.kill();
        }

        // Wait for all servers to shutdown
        for (Server server : serverThreads.keySet()) {
            Thread thread = serverThreads.get(server);
            try {
                System.err.printf("S (%s) - %s awaiting...%n", server, thread.getName());
                thread.join();
                System.err.printf("S (%s) - %s finished...%n", server, thread.getName());
            } catch (InterruptedException e) {
                System.err.printf("S (%s) - %s interrupted...%n", server, thread.getName());
            }
        }
        System.out.printf("Servers down!%n");

        KeyHandler.cleanKeys();

        for (Server server : _servers) {
            System.out.printf("Blockchain State in server " + server.getID() + ": %s%n", server.getBlockChainState());
        }

        System.out.printf("Done!%n");
    }

    // Each line of the config file should be either (assuming all run in localhost IP):
        // [Number of byzantine processes] (ALWAYS the first line)
        // C [MESSAGE]
        // S [PORT]
        // #[COMMENT]
    public static void main(String[] args) throws IOException, IllegalStateException {
        if (!List.of(1, 2).contains(args.length) || (args.length == 2 && !args[1].equals("-debug"))) {
			System.out.printf("Argument(s) missing!%n");
			System.out.printf("Usage: java %s config_file [-debug]%n", InstanceManager.class.getName());
			return;
		}
        boolean debug = args.length == 2;

        if (!debug) {
            // Surpress link debug output
            PrintStream nullPrintStream = new PrintStream(OutputStream.nullOutputStream());
            System.setErr(nullPrintStream);
        }

        int id = 0, f;
        List<Server> servers = new ArrayList<>();
        List<Client> clients = new ArrayList<>();

        // Parse input file
        try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
            String line = br.readLine();
            f = Integer.parseInt(line);

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
                    id++;
                }
            }
        }

        setSystemParameters(clients, servers, f);

        runSystem();
    }
}
