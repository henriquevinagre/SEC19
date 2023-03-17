package pt.ulisboa.tecnico.sec.ibft;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BlockchainNode {
    private int _clientId;
    private String _value;

    public BlockchainNode(int clientId, String value) {
        _clientId = clientId;
        _value = value;
    }

    public int getClientId() {
        return _clientId;
    }

    public String getAppendString() {
        return _value;
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(_clientId);
        dos.writeUTF(_value);

        return baos.toByteArray();
    }

    public static BlockchainNode fromDataInputStream(DataInputStream dis) throws IOException {
        int clientId = dis.readInt();
        String value = dis.readUTF();

        return new BlockchainNode(clientId, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BlockchainNode)) return false;
        BlockchainNode bcv = (BlockchainNode) obj;
        return bcv.getClientId() == this.getClientId()
            && bcv.getAppendString().equals(this.getAppendString());
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = 31 * result + _value.hashCode();
        result = 31 * result + _clientId;

        return result;
    }

    @Override
    public String toString() {
        return String.format("[client:%d; value:'%s']", _clientId, _value);
    }
}
