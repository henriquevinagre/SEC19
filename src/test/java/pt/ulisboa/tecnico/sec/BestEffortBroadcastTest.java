package pt.ulisboa.tecnico.sec;

import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pt.ulisboa.tecnico.sec.broadcasts.BestEffortBroadcast;
import pt.ulisboa.tecnico.sec.ibft.HDLProcess;
import pt.ulisboa.tecnico.sec.instances.InstanceManager;
import pt.ulisboa.tecnico.sec.links.AuthenticatedPerfectLink;
import pt.ulisboa.tecnico.sec.messages.ClientRequestMessage;
import pt.ulisboa.tecnico.sec.messages.LinkMessage;
import pt.ulisboa.tecnico.sec.messages.Message;

/**
 * Unit test for best effort broadcast.
 */
public class BestEffortBroadcastTest {
    private static HDLProcess p1;
    private static HDLProcess p2;
    private static HDLProcess p3;

    @Before
    public void setup() throws UnknownHostException {
        p1 = new HDLProcess(0);
        p2 = new HDLProcess(1);
        p3 = new HDLProcess(2);
        InstanceManager.setSystemParameters(List.of(p1, p2, p3));

        // Surpress link debug output
        PrintStream nullPrintStream = new PrintStream(OutputStream.nullOutputStream());
        System.setErr(nullPrintStream);
    }

    // @Test
    // public void checkCreate() {
    //     BestEffortBroadcast beb = new BestEffortBroadcast(new AuthenticatedPerfectLink(p1), InstanceManager.getServerProcesses());

    //     assertTrue(beb.getChannel().getChannelOwner() == p1);

    //     List<HDLProcess> allProcesses = beb.getInteractServers();

    //     assertTrue("Any process must be able to p1 interact with", allProcesses.contains(p1) && 
    //         allProcesses.contains(p2) && allProcesses.contains(p3));

    //     beb.close();
    // }

    // @Test
    // public void checkComunication() throws InterruptedException {
    //     BestEffortBroadcast beb1 = new BestEffortBroadcast(new AuthenticatedPerfectLink(p1), InstanceManager.getServerProcesses());
    //     BestEffortBroadcast beb2 = new BestEffortBroadcast(new AuthenticatedPerfectLink(p2), InstanceManager.getServerProcesses());
    //     BestEffortBroadcast beb3 = new BestEffortBroadcast(new AuthenticatedPerfectLink(p3), InstanceManager.getServerProcesses());

    //     // p1 prepares request
    //     String value = "Hello all!";
    //     ClientRequestMessage messageToAnyOne = new ClientRequestMessage(value);

    //     // p2 and p3 execution waiting for the 
    //     BroadcastDeliverExecution p2Execution = new BroadcastDeliverExecution(beb2);
    //     BroadcastDeliverExecution p3Execution = new BroadcastDeliverExecution(beb3);
    //     Thread p2Thread = new Thread(p2Execution);
    //     Thread p3Thread = new Thread(p3Execution);
    //     p2Thread.start();
    //     p3Thread.start();

    //     // p1 sends message to p2
    //     beb1.broadcast(messageToAnyOne);

    //     // p1 waits for p2 and p3 delivers its message
    //     p2Thread.join();
    //     p3Thread.join();
        
    //     // check received message
    //     LinkMessage receivedMessageP2 = p2Execution.getReceivedMessage();
    //     LinkMessage receivedMessageP3 = p3Execution.getReceivedMessage();

    //     assertTrue("Incorrect sender for p2", receivedMessageP2.getSender() == p1);
    //     assertTrue("Incorrect sender for p3", receivedMessageP3.getSender() == p1);

    //     assertTrue("p2 should receive a client request", 
    //         receivedMessageP2.getMessage().getMessageType().equals(Message.MessageType.CLIENT_REQUEST));
        
    //     assertTrue("p3 should receive a client request", 
    //         receivedMessageP3.getMessage().getMessageType().equals(Message.MessageType.CLIENT_REQUEST));
        
    //     ClientRequestMessage p2Message = (ClientRequestMessage) receivedMessageP2.getMessage();
    //     ClientRequestMessage p3Message = (ClientRequestMessage) receivedMessageP3.getMessage();

    //     assertTrue("Received message differs from the one p1 sent", p2Message.getValue().equals(value));
    //     assertTrue("Received message differs from the one p1 sent", p3Message.getValue().equals(value));

    //     beb1.close();
    //     beb2.close();
    //     beb3.close();
    // }

    @After
    public void cleanup() {
        // Reset link debug output
        System.setErr(System.err);
    }
}
