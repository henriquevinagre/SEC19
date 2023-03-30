package pt.ulisboa.tecnico.sec.tes;

import java.util.List;

import pt.ulisboa.tecnico.sec.tes.transactions.Transaction;
import pt.ulisboa.tecnico.sec.tes.transactions.TransferTransaction;
import pt.ulisboa.tecnico.sec.tes.transactions.Transaction.TESOperation;

import java.security.PublicKey;
import java.util.ArrayList;

// Token Exchange System supported by a State Machine Replication System
public class TESState {

    List<TESAccount> _accounts;

    public TESState() {
        _accounts = new ArrayList<>();
    }

    public boolean isEmpty() {
        return _accounts.isEmpty();
    }

    public void addAccount(TESAccount account) {
        _accounts.add(account);
    }

    public TESAccount getAccount(PublicKey key) {
        return _accounts.stream().filter(a -> a.getID().equals(key)).findFirst().orElse(null);
    }

    public boolean checkTransaction(Transaction transaction) {
        if (!transaction.validateTransaction())
            return false;
        
        TESOperation operation = transaction.getOperation();
        switch (operation) {
            case CREATE_ACCOUNT:
                return !_accounts.stream().filter((acc) -> acc.getID().equals(transaction.getSource())).findAny().isPresent();
            case TRANSFER:
                TransferTransaction t = (TransferTransaction) transaction;
                TESAccount sourceAccount, destinationAccount;
                if ((sourceAccount = _accounts.stream().filter((acc) -> acc.getID().equals(t.getSource())).findFirst().orElse(null)) == null
                    || (destinationAccount = _accounts.stream().filter((acc) -> acc.getID().equals(t.getDestination())).findFirst().orElse(null)) == null)
                    return false;
                
                double amount = t.getAmount();
                return sourceAccount.getTucs() >= amount && amount < Double.MAX_VALUE - destinationAccount.getTucs();
                
            // ...

            default:
                return false; // Not a valid operation
        }
    }

    @Override
    public String toString() {
        return "TES\n\t-> " + String.join("\n\t-> ", _accounts.stream().map((acc) -> acc.toString()).toArray(CharSequence[]::new));
    }
}