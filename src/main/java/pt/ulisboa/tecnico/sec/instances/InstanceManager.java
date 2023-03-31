package pt.ulisboa.tecnico.sec.instances;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.ulisboa.tecnico.sec.crypto.KeyHandler;
import pt.ulisboa.tecnico.sec.ibft.HDLProcess;
import pt.ulisboa.tecnico.sec.instances.commands.CreateAccountCommand;
import pt.ulisboa.tecnico.sec.instances.commands.CheckBalanceCommand;
import pt.ulisboa.tecnico.sec.instances.commands.TransferCommand;
import pt.ulisboa.tecnico.sec.instances.commands.InvalidCommandException;
import pt.ulisboa.tecnico.sec.tes.TESAccount;
import pt.ulisboa.tecnico.sec.utils.Logger;

public class InstanceManager {

    private static List<Server> _servers = new ArrayList<>();
    private static List<Client> _clients = new ArrayList<>();
    private static int _numByzantineProcesses;
    private static int _quorum;

    private static List<HDLProcess> _systemsProcesses = new ArrayList<>();

    public static void setSystemParameters(List<HDLProcess> processes) {
        KeyHandler.cleanKeys();
        for (HDLProcess p: processes) {
            KeyHandler.generateKeyPair(p.getID());
        }
        for (int i = 0; i < processes.size(); i++) {
            for (int j = i; j < processes.size(); j++) {
                KeyHandler.generateKeyFor(processes.get(i).getID(), processes.get(j).getID());
            }
        }
        _systemsProcesses = processes;
    }

    public static void setSystemParameters(List<Client> clients, List<Server> servers, int f) {
        // Keep process instances
        _clients = clients;
        _servers = servers;

        // Prepare static system information

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

    public static List<HDLProcess> getSystemProcesses() {
        return _systemsProcesses;
    }

    public static List<HDLProcess> getAllParticipants() {
        return _servers.stream().map(s -> (HDLProcess) s).collect(Collectors.toList());
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

    public static int getQuorum() {
        return _quorum;
    }

    public static void runSystem() {
        if (_clients.isEmpty() || _servers.isEmpty())
            throw new IllegalArgumentException("No client or server processes were created");

        // Self execution of the system through client requests

        Map<Server, Thread> serverThreads = new HashMap<>();
        for (Server server : _servers) {
            Thread t = new Thread(() -> {
                server.execute();
            });
            t.start();
            serverThreads.put(server, t);
        }

        for (Server s1 : _servers) {
            for (Server s2 : _servers) {
                s1.getTESState().addAccount(new TESAccount(s2.getPublicKey()));
            }
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

        System.out.println("Clients terminated");

        // Signal servers to shutdown now
        System.out.printf("Shutting down the servers...%n");
        for (Server server :_servers) {
            server.kill();
        }

        // Wait for all servers to shutdown
        for (Server server : serverThreads.keySet()) {
            Thread thread = serverThreads.get(server);
            try {
                Logger.Logln(String.format("S (%s) - %s awaiting...", server, thread.getName()));
                thread.join();
                System.err.printf("S (%s) - %s finished...%n", server, thread.getName());
            } catch (InterruptedException e) {
                System.err.printf("S (%s) - %s interrupted...%n", server, thread.getName());
            }
        }
        Logger.Logln("Servers down!", System.out);

        KeyHandler.cleanKeys();

        for (Server server : _servers) {
            Logger.Logln("Blockchain State in server " + server.getID() + ": " + server.getBlockChainState(), System.out);
        }

        Logger.Logln("Done!");
    }

    // Each line of the config file should be either (assuming all run in localhost IP):
        // [Number of byzantine processes] (ALWAYS the first line)
        // C [MESSAGE]
        // S [PORT]
        // #[COMMENT]
    public static void main(String[] args) throws IOException, IllegalStateException {
        if (!List.of(1, 2).contains(args.length) || (args.length == 2 && !args[1].equals("-debug"))) {
			Logger.Logln("Argument(s) missing!", System.out);
			System.out.printf("Usage: java %s config_file [-debug]%n", InstanceManager.class.getName());
			return;
		}
        boolean debug = args.length == 2;

        if (!debug) {
            // Surpress link debug output
            PrintStream nullPrintStream = new PrintStream(OutputStream.nullOutputStream());
            System.setErr(nullPrintStream);
        }

        int f;
        List<Server> servers = new ArrayList<>();
        List<Client> clients = new ArrayList<>();

        // Parse input file
        try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
            String line = br.readLine();
            f = Integer.parseInt(line);

            while ((line = br.readLine()) != null) {
                if (!line.isEmpty()) {
                    try {
                        switch (line.charAt(0)) {
                            case '#':   // commentary
                                break;
                            case 'C':   // create client > C <id>
                                addClient(clients, line);
                                break;
                            case 'S':   // create server > S <id> <port>
                                addServer(servers, line);
                                break;
                            case 'T':   // create command > T <type> <sender> [other] [amount]
                                addTransaction(clients, line);
                                break;
                            default:
                                Logger.Logln("Unknown command on line + '" + line + "'");
                                break;
                        }
                    } catch (InvalidCommandException e) {
                        Logger.Log("Ignoring line '" + line + "', as it gave following exception:", System.out);
                    } catch (Exception e) {
                        Logger.Log(e, System.out);
                        e.printStackTrace();
                    }
                }
            }
        }

        setSystemParameters(clients, servers, f);

        runSystem();
        System.exit(0);
    }

    private static boolean checkLineFormat(String line, String pattern) {
        String regex = String.format("^%s$", pattern);
        return line.matches(regex);
    }

    private static void addClient(List<Client> clients, String line) throws NumberFormatException, UnknownHostException, InvalidCommandException {
        if (!checkLineFormat(line, "C [0-9]+")) {
            throw new InvalidCommandException(line, "Add Client");
        }

        String[] args = line.split(" ");
        int id = Integer.valueOf(args[1]);
        clients.add(new Client(id));
    }

    private static void addServer(List<Server> servers, String line) throws NumberFormatException, UnknownHostException, InvalidCommandException {
        if (!checkLineFormat(line, "S [0-9]+ [0-9]{4}")) {
            throw new InvalidCommandException(line, "Add Server");
        }

        String[] args = line.split(" ");
        int id = Integer.valueOf(args[1]);
        int port = Integer.valueOf(args[2]);
        if (port < 1024) throw new NumberFormatException("Port must be above 1023.");

        servers.add(new Server(id, port));
    }

    private static void addCreateAccountTransaction(List<Client> clients, String line) throws NumberFormatException, InvalidCommandException {
        if (!checkLineFormat(line, "T C [0-9]+")) {
            throw new InvalidCommandException(line, "Create Account Transaction");
        }
        
        String[] args = line.split(" ");
        int id = Integer.valueOf(args[2]);

        for (Client client : clients) {
            if (client.getID() == id) {
                client.addCommand(new CreateAccountCommand(client));
                return;
            }
        }

        throw new InvalidCommandException("No client with id '" + id + "'");
    }
    
    private static void addTransferTransaction(List<Client> clients, String line) throws NumberFormatException, InvalidCommandException {
        if (!checkLineFormat(line, "T T [0-9]+ [0-9]+ [0-9]+")) {
            throw new InvalidCommandException(line, "Transfer Transaction");
        }
        
        String[] args = line.split(" ");
        int senderID = Integer.valueOf(args[2]);
        int receiverID = Integer.valueOf(args[3]);
        double amount = Double.valueOf(args[4]);

        Client sender = clients.stream().filter((c) -> c.getID() == senderID).findAny().orElse(null);
        Client receiver = clients.stream().filter((c) -> c.getID() == receiverID).findAny().orElse(null);
        
        if (sender == null) {
            throw new InvalidCommandException("Sender has id '" + senderID + "', but no client with that id exists!");
        } else if (receiver == null) {
            throw new InvalidCommandException("Receiver has id '" + receiverID + "', but no client with that id exists!");
        }

        sender.addCommand(new TransferCommand(sender, receiver, amount));
    }

    private static void addCheckBalanceTransaction(List<Client> clients, String line) throws NumberFormatException, InvalidCommandException {
        if (!checkLineFormat(line, "T B [0-9]+ [0-9]+")) {
            throw new InvalidCommandException(line, "Check Balance Transaction");
        }

        String[] args = line.split(" ");
        int senderID = Integer.valueOf(args[2]);
        int ownerID = Integer.valueOf(args[3]);

        Client sender = clients.stream().filter((c) -> c.getID() == senderID).findAny().orElse(null);
        Client owner = clients.stream().filter((c) -> c.getID() == ownerID).findAny().orElse(null);
        
        if (sender == null) {
            throw new InvalidCommandException("Sender has id '" + senderID + "', but no client with that id exists!");
        } else if (owner == null) {
            throw new InvalidCommandException("Owner has id '" + ownerID + "', but no client with that id exists!");
        }

        sender.addCommand(new CheckBalanceCommand(sender, owner));
    }

    private static void addTransaction(List<Client> clients, String line) throws NumberFormatException, InvalidCommandException {
        if (!checkLineFormat(line, "T [CTB] [0-9]+( [0-9]+){0,2}")) {
            throw new InvalidCommandException(line, "Add Transaction");
        }

        switch (line.split(" ")[1]) {
            case "C":
                addCreateAccountTransaction(clients, line);
                break;
            case "T":
                addTransferTransaction(clients, line);
                break;
            case "B":
                addCheckBalanceTransaction(clients, line);
                break;
            default:
                throw new InvalidCommandException(line, "WTF how did we get here???");
        }
    }
}