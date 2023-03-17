package pt.ulisboa.tecnico.sec;

import pt.ulisboa.tecnico.sec.links.Channel;
import pt.ulisboa.tecnico.sec.messages.LinkMessage;

public class ChannelDeliverExecution implements Runnable {

    private final Channel _channelInstance;
    
    private volatile LinkMessage _receivedMessage;

    public ChannelDeliverExecution(Channel channelInstance) {
        _channelInstance = channelInstance;
    }

    @Override
    public void run() {
        try {
            // _receivedMessage = _channelInstance.deliver();
        } catch (Exception e) {
            _receivedMessage = null;
        }
    }

    public LinkMessage getReceivedMessage() { return _receivedMessage; }


}
