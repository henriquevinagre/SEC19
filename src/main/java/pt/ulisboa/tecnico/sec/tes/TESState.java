package pt.ulisboa.tecnico.sec.tes;

import java.util.List;
import java.security.PublicKey;
import java.util.ArrayList;

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
        for (TESAccount account : _accounts) {
            if (account.getID().equals(key)) {
                return account;
            }
        }

        return null;
    }
}