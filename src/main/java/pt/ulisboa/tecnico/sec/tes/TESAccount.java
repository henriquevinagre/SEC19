package pt.ulisboa.tecnico.sec.tes;

import java.security.PublicKey;

import pt.ulisboa.tecnico.sec.crypto.KeyHandler;

// Account of the TES
public class TESAccount {

    private static final double INIT_BALANCE = 100f;

    private PublicKey _id;
    private double _tucs;   // balance

    public TESAccount(PublicKey id) {
        _id = id;
        _tucs = INIT_BALANCE;
    }

    public PublicKey getID() {
        return _id;
    }

    public String getIDBase64() {
        return KeyHandler.KeyBase64(_id);
    }

    public String getIDB64Readable() {
        return KeyHandler.KeyBase64Readable(_id);
    }

    public double getTucs() {
        return _tucs;
    }

    public double addBalance(double amount) {
        return _tucs += amount;
    }

    public double subtractBalance(double amount) {
        return _tucs -= amount;
    }

    public double setBalance(double tucs) {
        return _tucs = tucs;
    }

    public TESAccount copy() {
        TESAccount newAccount = new TESAccount(this._id);

        newAccount.setBalance(this._tucs);

        return newAccount;
    }

    @Override
    public String toString() {
        return String.format("Account<" + this.getIDB64Readable() + ", %f>", this.getTucs());
    }

}