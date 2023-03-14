package pt.tecnico.ibft;

public class BlockchainState {

    private String _value;

    public BlockchainState(String value) {
        _value = value;
    }

    public String getValue() {
        return _value;
    }

    public void append(String newValue) {
        _value = _value + newValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BlockchainState)) return false;
        BlockchainState bs = (BlockchainState) obj;
        return bs.getValue().equals(this.getValue());
    }
}
