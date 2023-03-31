package pt.ulisboa.tecnico.sec.tes;

import java.util.List;

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

    @Override
    public String toString() {
        return "TES\n\t-> " + String.join("\n\t-> ", _accounts.stream().map((acc) -> acc.toString()).toArray(CharSequence[]::new));
    }
}