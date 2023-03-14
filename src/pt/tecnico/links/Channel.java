package pt.tecnico.links;

import pt.tecnico.ibft.HDLProcess;
import pt.tecnico.messages.LinkMessage;

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
