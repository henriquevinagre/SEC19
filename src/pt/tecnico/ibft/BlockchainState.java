package pt.tecnico.ibft;

import java.util.ArrayList;
import java.util.List;

public class BlockchainState {

    private List<BlockchainNode> _state;

    public BlockchainState() {
        _state = new ArrayList<>();
    }

    public String getValue() {
        String res = "";

        for (BlockchainNode value : _state) {
            res += value.getAppendString();
        }

        return res;
    }

    public void append(BlockchainNode newBlock) {
        _state.add(newBlock);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BlockchainState)) return false;
        BlockchainState bs = (BlockchainState) obj;
        return bs.getValue().equals(this.getValue());
    }
}
