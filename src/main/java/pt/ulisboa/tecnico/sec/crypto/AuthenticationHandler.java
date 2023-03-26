package pt.ulisboa.tecnico.sec.crypto;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import pt.ulisboa.tecnico.sec.messages.Message;

public class AuthenticationHandler {

    private AuthenticationHandler() throws IllegalStateException {
        throw new IllegalStateException("Utility class");
    }


    public static String getMessageMAC(SecretKey key, Message message) throws IllegalStateException {
        byte[] macBytes; // new MAC according to the given secret
        try {
            Mac mac = Mac.getInstance("HmacSHA512"); // HMAC
            mac.init(key);
            mac.update(message.getDataBytes());
            macBytes = mac.doFinal();
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalStateException | IOException e) {
            throw new IllegalStateException();
        }
        return Base64.getEncoder().encodeToString(macBytes);
    }

    public static boolean checkMAC(SecretKey key, Message message) throws IllegalStateException {
        String messageMAC = message.getMAC();
        boolean valid = false;
        byte[] dataMACBytes;
        try {
            Mac dataMAC = Mac.getInstance("HmacSHA512"); // HMAC
            dataMAC.init(key);
            dataMAC.update(message.getDataBytes());
            dataMACBytes = dataMAC.doFinal();
            valid = Base64.getEncoder().encodeToString(dataMACBytes).equals(messageMAC);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalStateException | IOException e) {
            e.printStackTrace(System.out);
            throw new IllegalStateException();
        }
        return valid;
    }

    public static String getMessageSignature(PrivateKey key, Message message) throws IllegalStateException {
        byte[] signatureBytes;
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            signature.update(message.getDataBytes());
            signatureBytes = signature.sign();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | IOException e) {
            throw new IllegalStateException();
        }
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    public static boolean checkSignature(PublicKey key, Message message) throws IllegalStateException {
        byte[] signatureBytes = Base64.getDecoder().decode(message.getSignature());
        boolean valid = false;
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key);
            verifier.update(message.getDataBytes());
            valid = verifier.verify(signatureBytes);
        } catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException | IOException e) {
            throw new IllegalStateException();
        }
        return valid;
    }



}
