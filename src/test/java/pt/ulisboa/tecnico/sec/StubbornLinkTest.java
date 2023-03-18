package pt.ulisboa.tecnico.sec;

import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pt.ulisboa.tecnico.sec.crypto.KeyHandler;
import pt.ulisboa.tecnico.sec.ibft.HDLProcess;
import pt.ulisboa.tecnico.sec.instances.InstanceManager;
import pt.ulisboa.tecnico.sec.links.StubbornLink;
import pt.ulisboa.tecnico.sec.messages.ClientRequestMessage;
import pt.ulisboa.tecnico.sec.messages.LinkMessage;
import pt.ulisboa.tecnico.sec.messages.Message;

/**
 * Unit test for stubborn point to point link.
 */
public class StubbornLinkTest {
    private HDLProcess p1;
    private HDLProcess p2;
    private StubbornLink sl1;
    private StubbornLink sl2;

    @Before
    public void setup() throws UnknownHostException {
        // Surpress link debug output
        PrintStream nullPrintStream = new PrintStream(OutputStream.nullOutputStream());
        System.setErr(nullPrintStream);
        
        p1 = new HDLProcess(0);
        p2 = new HDLProcess(1);

        InstanceManager.setSystemParameters(List.of(p1, p2));

        sl1 = new StubbornLink(p1);
        sl2 = new StubbornLink(p2);
    }

    @Test
    public void checkCreate() {
        assertTrue(sl1.getChannelOwner() == p1);
        assertTrue(sl2.getChannelOwner() == p2);
    }

    @Test
    public void checkComunication() throws InterruptedException {
        // p1 prepares request
        String value = "Hello p2!";
        ClientRequestMessage p1Message = new ClientRequestMessage(value);
        LinkMessage request = new LinkMessage(p1Message, p1, p2);

        // p2 waiting for a message
        ChannelDeliverExecution p2Execution = new ChannelDeliverExecution(sl2);
        Thread p2Thread = new Thread(p2Execution);
        p2Thread.start();

        // p1 sends message to p2
        sl1.send(request);

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
    }

    @After
    public void cleanup() {
        // Close program instance
        sl1.close();
        sl2.close();
        KeyHandler.cleanKeys();
        System.setErr(System.err);
    }
}
