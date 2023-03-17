package pt.ulisboa.tecnico.sec;

import pt.ulisboa.tecnico.sec.broadcasts.BestEffortBroadcast;
import pt.ulisboa.tecnico.sec.messages.LinkMessage;

public class BroadcastDeliverExecution implements Runnable {

    private final BestEffortBroadcast _broadcastInstance;
    
    private volatile LinkMessage _receivedMessage;

    public BroadcastDeliverExecution(BestEffortBroadcast broadcastInstance) {
        _broadcastInstance = broadcastInstance;
    }

    @Override
    public void run() {
        try {
            _receivedMessage = _broadcastInstance.deliver();
        } catch (Exception e) {
            _receivedMessage = null;
        }
    }

    public LinkMessage getReceivedMessage() { return _receivedMessage; }


}
