package pt.tecnico.instances;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import pt.tecnico.crypto.KeyHandler;

public class InstanceManager {

    private static List<Server> servers = new ArrayList<>();
    private static List<Client> clients = new ArrayList<>();
    private static int byzantineProcesses;
    private static int quorum;

    public static List<HDLProcess> getServerProcesses() {
        List<HDLProcess> result = new ArrayList<>();
        for(Server server : servers) {
            result.add(server.getHDLInstance());
        }
        return result;
    }

    public static HDLProcess getLeader(int consensusInstance, int round) {
        return servers.get((consensusInstance + round) % servers.size()).getHDLInstance();
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
    public static void main(String[] args) throws IOException {
        if (!List.of(1, 2).contains(args.length) || (args.length == 2 && !args[1].equals("-debug"))) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s config_file [-debug]%n", InstanceManager.class.getName());
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
                if(!line.isEmpty()) {
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

        quorum = servers.size() - byzantineProcesses;

        // #pragma omp parallel for
        for (Server server : servers) {
            new Thread(() -> {
                try {
                    server.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }

        List<Thread> threads = new ArrayList<>();
        for (Client client : clients) {
            Thread t = new Thread(() -> {
                try {
                    client.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            threads.add(t);
            t.start();
        }

        // Wait for all clients to finish
        for (Thread thread : threads) {
            try {
                System.err.println(thread.getName());
                thread.join();
                System.err.println(thread.getName() + " finished");
            } catch (InterruptedException e) {
                thread.interrupt();
                e.printStackTrace();
            }
        }

        // TODO kaboom
        // for (Server server :servers) {
        //     server.kill();
        // }

        KeyHandler.cleanKeys();
    }
}
