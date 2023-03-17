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

    protected abstract void send(LinkMessage message) throws Exception;

    protected abstract LinkMessage deliver() throws Exception;

    protected abstract void close() throws Exception;

}
