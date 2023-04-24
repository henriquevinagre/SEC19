package pt.ulisboa.tecnico.sec.instances;

import java.net.UnknownHostException;
import java.security.SecureRandom;

import pt.ulisboa.tecnico.sec.ibft.HDLProcess;

public abstract class ByzantineProcess extends HDLProcess {

    public static final Integer TIMEOUT = 1500;
    private boolean actingByzantine = false;
    private SecureRandom randomGenerator = new SecureRandom();

    public ByzantineProcess(int id, int port) throws UnknownHostException {
        super(id, port);
    }

    public boolean startByzantineBehaviour() {
        actingByzantine = true;

        new Thread(() -> {
            try {
                Thread.sleep(TIMEOUT);
                while (actingByzantine) {
                    ByzantineHandler.invoke(this);
                    Thread.sleep(TIMEOUT);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                actingByzantine = false;
            }
        }).start();

        return actingByzantine;
    }
    
    protected SecureRandom getRandomGenerator() {
        return this.randomGenerator;
    }

    public void stopByzantineBehaviour() {
        actingByzantine = false;
    }

    public abstract void executeByzantineBehaviour(ByzantineBehaviour behaviour);
}