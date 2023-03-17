package pt.ulisboa.tecnico.sec;

import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pt.ulisboa.tecnico.sec.ibft.HDLProcess;
import pt.ulisboa.tecnico.sec.instances.InstanceManager;
import pt.ulisboa.tecnico.sec.links.AuthenticatedPerfectLink;
import pt.ulisboa.tecnico.sec.messages.ClientRequestMessage;
import pt.ulisboa.tecnico.sec.messages.LinkMessage;
import pt.ulisboa.tecnico.sec.messages.Message;

/**
 * Unit test for authenticated perfect point to point link.
 */
public class AuthenticatedLinkTest  {
    private static HDLProcess p1;
    private static HDLProcess p2;

    @Before
    public void setup() throws UnknownHostException {
        p1 = new HDLProcess(0);
        p2 = new HDLProcess(1);
        // InstanceManager.setSystemParameters(List.of(p1, p2));

        // Surpress link debug output
        PrintStream nullPrintStream = new PrintStream(OutputStream.nullOutputStream());
        System.setErr(nullPrintStream);
    }

    @Test
    public void checkCreate() {
        AuthenticatedPerfectLink pl1 = new AuthenticatedPerfectLink(p1);
        AuthenticatedPerfectLink pl2 = new AuthenticatedPerfectLink(p2);

        assertTrue(pl1.getChannelOwner() == p1);
        assertTrue(pl2.getChannelOwner() == p2);

        pl1.close();
        pl2.close();
    }

    @Test
    public void checkComunication() throws InterruptedException {
        AuthenticatedPerfectLink pl1 = new AuthenticatedPerfectLink(p1);
        AuthenticatedPerfectLink pl2 = new AuthenticatedPerfectLink(p2);

        // p1 prepares request
        String value = "Hello p2!";
        ClientRequestMessage p1Message = new ClientRequestMessage(value);
        LinkMessage request = new LinkMessage(p1Message, p1, p2);

        // p2 waiting for a message
        ChannelDeliverExecution p2Execution = new ChannelDeliverExecution(pl2);
        Thread p2Thread = new Thread(p2Execution);
        p2Thread.start();

        // p1 sends message to p2
        pl1.send(request);

        // p1 waits for p2 delivers its message
        p2Thread.join();
        
        // check received message
        LinkMessage receivedMessage = p2Execution.getReceivedMessage();

        assertTrue("Incorrect sender", receivedMessage.getSender() == p1);
        assertTrue("Terminated flag may not be on", receivedMessage.getTerminate() == false);

        assertTrue("Receive message should be a client request", 
            receivedMessage.getMessage().getMessageType().equals(Message.MessageType.CLIENT_REQUEST));
        
        ClientRequestMessage p2Message = (ClientRequestMessage) receivedMessage.getMessage();

        assertTrue("Received message differs from the one p1 sent", p2Message.getValue().equals(value));

        pl1.close();
        pl2.close();
    }

    @After
    public void cleanup() {
        // Reset link debug output
        System.setErr(System.err);
    }
}
