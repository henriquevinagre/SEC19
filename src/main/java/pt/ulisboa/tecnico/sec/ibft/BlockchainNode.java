package pt.ulisboa.tecnico.sec.ibft;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.sec.tes.Transaction;

public class BlockchainNode {
    public static final int NODE_SIZE = 10;
    public static final int TRANSACTION_FEE = 1; // every transaction must pay 1 coin to the block producer
    private List<Transaction> transactions;
    private List<Transaction> rewards;

    public BlockchainNode() {
        transactions = new ArrayList<>();
    }

    public BlockchainNode(List<Transaction> transactions, List<Transaction> rewards) {
        this.transactions = transactions;
        this.rewards = rewards;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void addTransaction(Transaction t, PublicKey producerKey) {
        if (transactions.size() == NODE_SIZE) throw new IllegalStateException("Node is full, cannot add more transactions!");
        transactions.add(t);

        // no need to sign these transactions, get scammed lmao
        rewards.add(Transaction.transferTransaction(t.getPublicKey(), producerKey, TRANSACTION_FEE));
    }

    public boolean isFull() {
        return transactions.size() == NODE_SIZE;
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        byte[] transactionBytes;

        for (Transaction t : transactions) {
            transactionBytes = t.toByteArray();
            dos.writeInt(transactionBytes.length);
            dos.write(transactionBytes);
        }

        dos.writeInt(0);

        for (Transaction t : rewards) {
            transactionBytes = t.toByteArray();
            dos.writeInt(transactionBytes.length);
            dos.write(transactionBytes);
        }

        dos.writeInt(0);

        return baos.toByteArray();
    }

    public static BlockchainNode fromDataInputStream(DataInputStream dis) throws IOException {
        List<Transaction> transactions = new ArrayList<>();
        List<Transaction> rewards = new ArrayList<>();

        int lenght = dis.readInt();
        byte[] bytes = null;

        while (lenght != 0) {
            bytes = dis.readNBytes(lenght);
            transactions.add(Transaction.fromByteArray(bytes));
            lenght = dis.readInt();
        }

        lenght = dis.readInt();

        while (lenght != 0) {
            bytes = dis.readNBytes(lenght);
            rewards.add(Transaction.fromByteArray(bytes));
            lenght = dis.readInt();
        }

        return new BlockchainNode(transactions, rewards);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BlockchainNode)) return false;
        BlockchainNode bcv = (BlockchainNode) obj;
        return bcv.getTransactions().equals(this.getTransactions());
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = 31 * result + transactions.hashCode();

        return result;
    }

    @Override
    public String toString() {
        String res = "";
        for (Transaction t : transactions)
            res += t.toString() + "\\";
        return String.format("[transactions:'%s']", res);
    }
}
