package pt.ulisboa.tecnico.sec.instances;

public class ByzantineHandler {

    public static void invoke(ByzantineProcess process) {
        process.executeByzantineBehaviour(ByzantineBehaviour.randomBehaviour());
    }
}