package pt.ulisboa.tecnico.sec.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.*;
import java.util.ArrayList;
import java.util.List;

public class KeyHandler {
    static final String PRIVATE_SUFFIX = ".key";
    static final String PUBLIC_SUFFIX = ".pub.key";

    static final List<File> keysFiles = new ArrayList<File>();

    private KeyHandler() throws IllegalStateException {
        throw new IllegalStateException("Utility class");
    }

    private static String getPrefix(int id) {
        return "keys/instance-" + id;
    }

    private static String getPrivateKeyFile(int id) {
        return getPrefix(id) + PRIVATE_SUFFIX;
    }

    private static String getPublicKeyFile(int id) {
        return getPrefix(id) + PUBLIC_SUFFIX;
    }

    public static void generateKey(int id) {
        generateKeyPair(getPrivateKeyFile(id), getPublicKeyFile(id));
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

            keysFiles.add(new File(publicPathName));
            keysFiles.add(new File(privatePathName));
        }
        catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException(String.format("[ERROR] Getting key pair"));
        }
    }

    public static PrivateKey getPrivateKey(int id) throws IllegalStateException {
        PrivateKey key = null;

        try {
            FileInputStream fis;
            fis = new FileInputStream(getPrivateKeyFile(id));
            byte[] keyBytes = fis.readAllBytes();
            fis.close();

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(keyBytes);
            key = KeyFactory.getInstance("RSA").generatePrivate(privateKeySpec);
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            throw new IllegalStateException(String.format("[ERROR] Getting private key for process %d", id));
        }

        return key;
    }

    public static PublicKey getPublicKey(int id) {
        PublicKey key = null;

        try {
            FileInputStream fis;
            fis = new FileInputStream(getPublicKeyFile(id));
            byte[] keyBytes = fis.readAllBytes();
            fis.close();

            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(keyBytes);
            key = KeyFactory.getInstance("RSA").generatePublic(publicKeySpec);
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            throw new IllegalStateException(String.format("[ERROR] Getting public key for process %d", id));
        }

        return key;
    }

    public static void cleanKeys() {
        for (File file : keysFiles) {
            file.delete();
        }
    }
}
