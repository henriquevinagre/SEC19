package pt.tecnico.crypto;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.*;

public class KeyHandler {
    private static String clientPubKey = "keys/client.pub.key";
    private static String clientPrivKey = "keys/client.key";
    private static String serverPubKey = "keys/server.pub.key";
    private static String serverPrivKey = "keys/server.key";

    private KeyHandler() {
        throw new IllegalStateException("Utility class");
    }

    public static void generateKeys() {
        generateKeyPair(clientPrivKey, clientPubKey);
        generateKeyPair(serverPrivKey, serverPubKey);
    }

    private static void generateKeyPair(String privatePathName, String publicPathName) {
        try {
            SecureRandom random;
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            random = SecureRandom.getInstance("SHA1PRNG");
            keyGen.initialize(2048, random);
            KeyPair pair = keyGen.generateKeyPair();
            PrivateKey privateKey = pair.getPrivate();
            PublicKey publicKey = pair.getPublic();

            FileOutputStream privateFos = new FileOutputStream(privatePathName);
            privateFos.write(privateKey.getEncoded());
            privateFos.close();

            FileOutputStream publicFos = new FileOutputStream(publicPathName);
            publicFos.write(publicKey.getEncoded());
            publicFos.close();
        }
        catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }

    public static PrivateKey getPrivateKey(String pathName) throws IOException {
        PrivateKey key = null;

        try {
            FileInputStream fis;
            fis = new FileInputStream(pathName);
            byte[] keyBytes = fis.readAllBytes();
            fis.close();

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(keyBytes);
            key = KeyFactory.getInstance("RSA").generatePrivate(privateKeySpec);
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return key;
    }

    public static PublicKey getPublicKey(String pathName) throws IOException {
        PublicKey key = null;

        try {
            FileInputStream fis;
            fis = new FileInputStream(pathName);
            byte[] keyBytes = fis.readAllBytes();
            fis.close();

            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(keyBytes);
            key = KeyFactory.getInstance("RSA").generatePublic(publicKeySpec);
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return key;
    }
}
