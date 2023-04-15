package pt.ulisboa.tecnico.sec.blockchain;

import java.util.HashMap;
import java.util.Map;

public class BlockchainState {

    private Map<Integer, BlockchainNode> _state;

    public BlockchainState() {
        _state = new HashMap<>();
    }

    public void append(int instance, BlockchainNode newBlock) {
        _state.put(instance, newBlock);
    }

    public String getRaw() {
        String res = "";

        for (int instance = 0; instance < _state.size(); instance++) {
            res += _state.getOrDefault(instance, new BlockchainNode()).toString();
        }

        return res;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BlockchainState)) return false;
        BlockchainState bs = (BlockchainState) obj;
        return bs.toString().equals(this.toString());
    }

    @Override
    public String toString() {
        String res = "";

        for (int instance = 0; instance < _state.size(); instance++) {
            res += String.format("| %s |", _state.getOrDefault(instance, new BlockchainNode()).toString());
        }

        return res;
    }
}
