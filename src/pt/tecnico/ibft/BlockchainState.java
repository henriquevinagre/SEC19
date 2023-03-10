package pt.tecnico.ibft;

public class BlockchainState {

    private final String _value;

    public BlockchainState(String value) {
        _value = value;
    }

    public String getValue() {
        return _value;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BlockchainState)) return false;
        BlockchainState bs = (BlockchainState) obj;
        return bs.getValue().equals(this.getValue());
    }
}
