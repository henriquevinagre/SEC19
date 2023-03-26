package pt.ulisboa.tecnico.sec.ibft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class ByzantineHandler {

    public enum ByzantineBehaviour {
        NONE,
        CRASH,                  // [B1]
        SKIPPING_ACKS,          // [B2]
        // INCOMPLETE_BROADCAST    // [B3] some problems
    }

    private static Map<HDLProcess, ByzantineBehaviour> _byzantineProcesses = new HashMap<>();
    private static List<HDLProcess> _activeProcessesBehaviours = new ArrayList<>();

    private ByzantineHandler() throws IllegalStateException {
        throw new IllegalStateException("Utility class");
    }

    public static void setParameters(List<HDLProcess> byzantines, List<ByzantineBehaviour> behaviours) {
        _byzantineProcesses = IntStream.range(0, byzantines.size()).boxed().collect(Collectors.toMap(byzantines::get, behaviours::get));
        _activeProcessesBehaviours.clear();
    }

    public static List<HDLProcess> getByzantines() {
        return _byzantineProcesses.keySet().stream().toList();
    }

    public static void activeAll() {
        for (HDLProcess byzantine: _byzantineProcesses.keySet()) {
            _activeProcessesBehaviours.add(byzantine);
        }
    }

    public static void activeOne(HDLProcess byzantine) {
        if (!_byzantineProcesses.containsKey(byzantine) || _activeProcessesBehaviours.contains(byzantine))
            return;
        
        _activeProcessesBehaviours.add(byzantine);
    }

    public static void activeOneWithBehaviour(HDLProcess byzantine, ByzantineBehaviour behaviour) {
        if (!_byzantineProcesses.containsKey(byzantine))
            return;
        
        _byzantineProcesses.put(byzantine, behaviour);
        activeOne(byzantine);
    }

    public static boolean withBehaviourActive(HDLProcess process, ByzantineBehaviour behaviour) {
        return _byzantineProcesses.containsKey(process) && _activeProcessesBehaviours.contains(process) &&
            _byzantineProcesses.get(process).equals(behaviour); 
    }
    
    public static ByzantineBehaviour getCurrentBehaviourFor(HDLProcess process) {
        if (!_byzantineProcesses.containsKey(process))
            return ByzantineBehaviour.NONE; // correct process

        if (!_activeProcessesBehaviours.contains(process))
            return ByzantineBehaviour.NONE; // byzantine process with inactive behaviour

        return _byzantineProcesses.get(process);
    }


}
