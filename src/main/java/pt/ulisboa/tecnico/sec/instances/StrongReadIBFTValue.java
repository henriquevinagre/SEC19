package pt.ulisboa.tecnico.sec.instances;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.PublicKey;

import pt.ulisboa.tecnico.sec.crypto.KeyHandler;
import pt.ulisboa.tecnico.sec.ibft.IBFTValueIT;

public class StrongReadIBFTValue implements IBFTValueIT {

    private Integer timestamp;
    private PublicKey clientKey;
    private Integer clientNonce;
    
    public StrongReadIBFTValue() {
        this.timestamp = -1;
        this.clientKey = null;
        this.clientNonce = -1;
    }

    public StrongReadIBFTValue(Integer timestamp, PublicKey clientKey, Integer clientNonce) {
        this.timestamp = timestamp;
        this.clientKey = clientKey;
        this.clientNonce = clientNonce;
    }

    public Integer getTimestamp() {
        return this.timestamp;
    }
    
    public PublicKey getClientKey() {
        return this.clientKey;
    }

    public Integer getNonce() {
        return this.clientNonce;
    }

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        dos.writeInt(timestamp);
        
        byte[] clientKeyBytes = clientKey.getEncoded();
        dos.writeInt(clientKeyBytes.length);
        dos.write(clientKeyBytes);

        dos.writeInt(clientNonce);

        return baos.toByteArray();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public StrongReadIBFTValue fromDataInputStream(DataInputStream dis) throws IOException {

        this.timestamp = dis.readInt();
        
        byte[] clientKeyBytes = new byte[dis.readInt()];
        dis.readFully(clientKeyBytes);

        try {
            this.clientKey = KeyHandler.deserializePublicKey(clientKeyBytes);
        } catch (IllegalStateException ile) {
            throw new IllegalStateException("[ERROR] Deserializing transaction destination key");
        }

        this.clientNonce = dis.readInt();

        return this;
    }

}
