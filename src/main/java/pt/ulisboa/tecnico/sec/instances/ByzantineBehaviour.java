package pt.ulisboa.tecnico.sec.instances;

import java.security.SecureRandom;

public enum ByzantineBehaviour {
    TERMINATE,
    INCOMPLETE_BROADCAST,
    REPLAY_MESSAGE;

    private static final SecureRandom random = new SecureRandom();

    public static ByzantineBehaviour randomBehaviour() {
        ByzantineBehaviour[] values = ByzantineBehaviour.values();
        return values[random.nextInt(values.length)];
    }
}