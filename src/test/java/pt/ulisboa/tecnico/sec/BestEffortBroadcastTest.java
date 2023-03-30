package pt.ulisboa.tecnico.sec;

import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pt.ulisboa.tecnico.sec.broadcasts.BestEffortBroadcast;
import pt.ulisboa.tecnico.sec.crypto.KeyHandler;
import pt.ulisboa.tecnico.sec.ibft.HDLProcess;
import pt.ulisboa.tecnico.sec.instances.InstanceManager;
import pt.ulisboa.tecnico.sec.links.AuthenticatedPerfectLink;
import pt.ulisboa.tecnico.sec.messages.ClientRequestMessage;
import pt.ulisboa.tecnico.sec.messages.LinkMessage;
import pt.ulisboa.tecnico.sec.messages.Message;
import pt.ulisboa.tecnico.sec.tes.transactions.CreateAccountTransaction;
import pt.ulisboa.tecnico.sec.tes.transactions.Transaction;

/**
 * Unit test for best effort broadcast.
 */
public class BestEffortBroadcastTest {
    private static HDLProcess p1;
    private static HDLProcess p2;
    private static HDLProcess p3;
    private static BestEffortBroadcast beb1;
    private static BestEffortBroadcast beb2;
    private static BestEffortBroadcast beb3;

    @Before
    public void setup() throws UnknownHostException {

        // Surpress link debug output
        PrintStream nullPrintStream = new PrintStream(OutputStream.nullOutputStream());
        System.setErr(nullPrintStream);

        p1 = new HDLProcess(0);
        p2 = new HDLProcess(1);
        p3 = new HDLProcess(2);
        InstanceManager.setSystemParameters(List.of(p1, p2, p3));

        beb1 = new BestEffortBroadcast(new AuthenticatedPerfectLink(p1), InstanceManager.getSystemProcesses());
        beb2 = new BestEffortBroadcast(new AuthenticatedPerfectLink(p2), InstanceManager.getSystemProcesses());
        beb3 = new BestEffortBroadcast(new AuthenticatedPerfectLink(p3), InstanceManager.getSystemProcesses());
    }

    @Test
    public void checkCreate() {

        assertTrue(beb1.getChannel().getChannelOwner() == p1);

        List<HDLProcess> allParticipants = beb1.getInteractProcesses();

        assertTrue("Any participant must be able for p1 interact with", allParticipants.contains(p1) && 
            allParticipants.contains(p2) && allParticipants.contains(p3));

    }

    @Test
    public void checkComunication() throws InterruptedException {

        // p1 prepares request
        PublicKey key = KeyHandler.generateAccountKeyPair().getPublic();
        Transaction t = new CreateAccountTransaction(key);
        ClientRequestMessage messageToAnyOne = new ClientRequestMessage(t);

        // p2 and p3 execution waiting for the
        BroadcastDeliverExecution p1Execution = new BroadcastDeliverExecution(beb1);
        BroadcastDeliverExecution p2Execution = new BroadcastDeliverExecution(beb2);
        BroadcastDeliverExecution p3Execution = new BroadcastDeliverExecution(beb3);
        Thread p1Thread = new Thread(p1Execution);
        Thread p2Thread = new Thread(p2Execution);
        Thread p3Thread = new Thread(p3Execution);
        p1Thread.start();
        p2Thread.start();
        p3Thread.start();

        // p1 sends message to any one
        beb1.broadcast(messageToAnyOne);

        // p1 waits for him, p2 and p3 delivers its message
        p1Thread.join();
        p2Thread.join();
        p3Thread.join();

        // check received message
        LinkMessage receivedMessageP1 = p1Execution.getReceivedMessage();
        LinkMessage receivedMessageP2 = p2Execution.getReceivedMessage();
        LinkMessage receivedMessageP3 = p3Execution.getReceivedMessage();

        assertTrue("Incorrect sender for p1", receivedMessageP1.getSender() == receivedMessageP1.getReceiver());
        assertTrue("Incorrect sender for p2", receivedMessageP2.getSender() == p1);
        assertTrue("Incorrect sender for p3", receivedMessageP3.getSender() == p1);

        assertTrue("p1 should receive its client request",
            receivedMessageP1.getMessage().getMessageType().equals(Message.MessageType.CLIENT_REQUEST));

        assertTrue("p2 should receive a client request",
            receivedMessageP2.getMessage().getMessageType().equals(Message.MessageType.CLIENT_REQUEST));

        assertTrue("p3 should receive a client request",
            receivedMessageP3.getMessage().getMessageType().equals(Message.MessageType.CLIENT_REQUEST));

        ClientRequestMessage p1Message = (ClientRequestMessage) receivedMessageP1.getMessage();
        ClientRequestMessage p2Message = (ClientRequestMessage) receivedMessageP2.getMessage();
        ClientRequestMessage p3Message = (ClientRequestMessage) receivedMessageP3.getMessage();

        assertTrue("Received message differs from the initial", p1Message.getTransaction().equals(t));
        assertTrue("Received message differs from the one p1 wanted to sent", p2Message.getTransaction().equals(t));
        assertTrue("Received message differs from the one p1 wanted to sent", p3Message.getTransaction().equals(t));
    }

    @After
    public void cleanup() {
        // Close program instance
        beb1.close();
        beb2.close();
        beb3.close();
        KeyHandler.cleanKeys();
        // Reset link debug output
        System.setErr(System.err);
    }
}
