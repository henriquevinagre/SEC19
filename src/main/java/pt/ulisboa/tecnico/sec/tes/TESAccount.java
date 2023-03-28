package pt.ulisboa.tecnico.sec.tes;

import java.security.PublicKey;

public class TESAccount {

    private static final double INIT_BALANCE = 5f;

    private PublicKey _id;
    private double _tucs;   // balance

    TESAccount(PublicKey id) {
        _id = id;
        _tucs = INIT_BALANCE;
    }

    public PublicKey getID() {
        return _id;
    }

    public double getTucs() {
        return _tucs;
    }

    protected double addBalance(double amount) {
        return _tucs += amount;
    }

    protected double subtractBalance(double amount) {
        return _tucs -= amount;
    }

    protected double setBalance(double tucs) {
        return _tucs = tucs;
    }

}