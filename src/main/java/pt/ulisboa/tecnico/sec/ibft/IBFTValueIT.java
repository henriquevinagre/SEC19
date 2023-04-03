package pt.ulisboa.tecnico.sec.ibft;

import java.io.DataInputStream;
import java.io.IOException;

public interface IBFTValueIT {

    // Serialization
    public byte[] toByteArray() throws IOException;
    public <T extends IBFTValueIT> T fromDataInputStream(DataInputStream dis) throws IOException;

    // ...
}