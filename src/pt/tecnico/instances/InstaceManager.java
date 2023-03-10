package pt.tecnico.instances;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Process;
import java.util.ArrayList;
import java.util.List;

import pt.tecnico.crypto.KeyHandler;

public class InstaceManager {

    private static List<Server> servers = new ArrayList<>();
    private static List<Client> clients = new ArrayList<>();

    public static List<HDLProcess> getServerProcesses() {
        List<HDLProcess> result = new ArrayList<>();
        for(Server server : servers) {
            result.add(server.getHDLInstance());
        }
        return result;
    }

    // each line of the config file should be either (assuming all run in localhost IP):
        // C [MESSAGE]
        // S [PORT]
        // #[COMMENT]
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s config_file%n", InstaceManager.class.getName());
			return;
		}

        int id = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
            String line;
            while ((line = br.readLine()) != null) {
                switch (line.charAt(0)) {
                    case 'C':
                        clients.add(new Client(id++, line.substring(2)));
                    case 'S':
                        servers.add(new Server(id++, Integer.parseInt(line.substring(2))));
                    default:
                        continue;
                }
            }
        }

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
                thread.join();
            } catch (InterruptedException e) {
                thread.interrupt();
                e.printStackTrace();
            }
        }

        for (Server server :servers) {
            server.kill();
        }
    }
}
