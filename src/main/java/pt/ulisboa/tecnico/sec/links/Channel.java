package pt.ulisboa.tecnico.sec.links;

import pt.ulisboa.tecnico.sec.ibft.HDLProcess;
import pt.ulisboa.tecnico.sec.messages.LinkMessage;

// Abstract Channel class
public abstract class Channel {
    
    protected HDLProcess owner;

    protected Channel(HDLProcess owner) {
        this.owner = owner;
    }

    public HDLProcess getChannelOwner() {
        return owner;
    }

    public abstract void send(LinkMessage message) throws IllegalStateException, InterruptedException;

    public abstract LinkMessage deliver() throws IllegalStateException, InterruptedException;

    public abstract void close();

}
