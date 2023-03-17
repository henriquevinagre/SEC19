package pt.ulisboa.tecnico.sec.ibft;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class BlockchainState {

    private Map<Integer, BlockchainNode> _state;
    //private List<BlockchainNode> _state;

    public BlockchainState() {
        _state = new HashMap<>();
        //_state = new ArrayList<>();
    }

    public void append(int instance, BlockchainNode newBlock) {
        _state.put(instance, newBlock);
        //_state.add(newBlock);
    }

    public String getRawString() {
        String res = "";

        for (int instance = 0; instance < _state.size(); instance++) {
        //for (BlockchainNode node : _state) {
            res += _state.getOrDefault(instance, new BlockchainNode(-1, "!!!NO MESSAGE!!!")).getAppendString();
            //res += node.getAppendString()
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
        //for (BlockchainNode node : _state) {
            res += String.format("| %s |", _state.getOrDefault(instance, new BlockchainNode(-1, "!!!NO MESSAGE!!!")).getAppendString());
            //res += String.format("| %s |", node.getAppendString());
        }

        return res;
    }
}
