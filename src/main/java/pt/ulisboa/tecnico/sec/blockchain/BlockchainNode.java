package pt.ulisboa.tecnico.sec.blockchain;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.sec.ibft.IBFTValueIT;
import pt.ulisboa.tecnico.sec.tes.transactions.Transaction;
import pt.ulisboa.tecnico.sec.tes.transactions.TransferTransaction;

public class BlockchainNode implements IBFTValueIT {
    public static final int FILL_TIMEOUT = 2000; // ms
    public static final int NODE_SIZE = 2;
    public static final int TRANSACTION_FEE = 1; // every transaction must pay 1 coin to the block producer
    private List<Transaction> transactions;
    private List<Transaction> rewards;

    public BlockchainNode() {
        transactions = new ArrayList<>();
        rewards = new ArrayList<>();
    }

    public BlockchainNode(List<Transaction> transactions, List<Transaction> rewards) {
        this.transactions = transactions;
        this.rewards = rewards;
    }

    public static BlockchainNode copy(BlockchainNode node) {
        return new BlockchainNode(new ArrayList<>(node.transactions), new ArrayList<>(node.rewards));
    }

    public BlockchainNode copy() {
        return new BlockchainNode(new ArrayList<>(this.transactions), new ArrayList<>(this.rewards));
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public List<Transaction> getRewards() {
        return rewards;
    }

    public void addTransaction(Transaction transaction, PublicKey producerKey) throws IllegalStateException {
        if (transactions.size() >= NODE_SIZE) 
            throw new IllegalStateException("Node is full, cannot add more transactions!");
        
        transactions.add(transaction);

        // no need to sign these transactions, get scammed lmao
        rewards.add(new TransferTransaction(transaction.getSource(), producerKey, TRANSACTION_FEE));
    }

    public boolean isFull() {
        return transactions.size() == NODE_SIZE;
    }

    public boolean isEmpty() {
        return transactions.isEmpty();
    }

    @Override
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

    @SuppressWarnings("unchecked")
    @Override
    public BlockchainNode fromDataInputStream(DataInputStream dis) throws IOException {
        this.transactions = new ArrayList<>();
        this.rewards = new ArrayList<>();

        int lenght = dis.readInt();
        byte[] bytes = null;

        while (lenght != 0) {
            bytes = dis.readNBytes(lenght);
            this.transactions.add(Transaction.fromByteArray(bytes));
            lenght = dis.readInt();
        }

        lenght = dis.readInt();

        while (lenght != 0) {
            bytes = dis.readNBytes(lenght);
            this.rewards.add(Transaction.fromByteArray(bytes));
            lenght = dis.readInt();
        }

        return this;
    }

    // public static BlockchainNode fromByteArray(byte[] bytes) throws IOException {
    //     ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    //     DataInputStream dis = new DataInputStream(bais);

    //     return BlockchainNode.fromDataInputStream(dis);
    // }

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
        //String res = String.join(" \\ ", transactions.stream().map((t) -> t.toString()).collect(Collectors.joining()));
        String res = String.join(" \\ ", transactions.stream().map((t) -> t.toString()).toArray(CharSequence[]::new));
        return String.format("[Transactions:'%s']", res);
    }
}
