package pt.ulisboa.tecnico.sec.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

public class AuthenticationHandler {

    // Macros for authentication purposes
    public static final Integer FAIL = -1;
    public static final Integer UNDEFINED = -2;
    public static final String UNDEFINED_HASH = "\0";

    private AuthenticationHandler() throws IllegalStateException {
        throw new IllegalStateException("Utility class");
    }

    public static String getMessageMAC(SecretKey key, byte[] dataBytes) throws IllegalStateException {
        byte[] macBytes; // new MAC according to the given secret
        try {
            Mac mac = Mac.getInstance("HmacSHA512"); // HMAC
            mac.init(key);
            mac.update(dataBytes);
            macBytes = mac.doFinal();
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalStateException e) {
            throw new IllegalStateException();
        }
        return Base64.getEncoder().encodeToString(macBytes);
    }

    public static boolean checkMAC(SecretKey key, String mac, byte[] dataBytes) throws IllegalStateException {
        boolean valid = false;
        byte[] dataMACBytes;
        try {
            Mac dataMAC = Mac.getInstance("HmacSHA512"); // HMAC
            dataMAC.init(key);
            dataMAC.update(dataBytes);
            dataMACBytes = dataMAC.doFinal();
            valid = Base64.getEncoder().encodeToString(dataMACBytes).equals(mac);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalStateException e) {
            e.printStackTrace(System.out);
            throw new IllegalStateException();
        }
        return valid;
    }
    public static String signBytes(PrivateKey key, byte[] dataBytes) throws IllegalStateException {
        byte[] signatureBytes;
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            signature.update(dataBytes);
            signatureBytes = signature.sign();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new IllegalStateException();
        }
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    public static boolean checkSignature(PublicKey key, String signature, byte[] dataBytes) throws IllegalStateException {
        byte[] signatureBytes = Base64.getDecoder().decode(signature);
        boolean valid = false;
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key);
            verifier.update(dataBytes);
            valid = verifier.verify(signatureBytes);
        } catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException();
        }
        return valid;
    }
}