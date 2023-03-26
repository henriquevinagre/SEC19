package pt.ulisboa.tecnico.sec.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.*;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public class KeyHandler {
    static final String KEYS_FOLDER = "keys";
    static final String PRIVATE_SUFFIX = ".prv.key";
    static final String PUBLIC_SUFFIX = ".pub.key";
    static final String SECRET_SUFFIX = ".key";

    static final List<File> keysFiles = new ArrayList<File>();

    private KeyHandler() throws IllegalStateException {
        throw new IllegalStateException("Utility class");
    }

    private static String getPrefix(int id) {
        return String.format("%s/instance-%d", KEYS_FOLDER, id);
    }

    private static String getSecretPrefix(int id1, int id2) {
        return String.format("%s/secret-%d-%d", KEYS_FOLDER, (id1 < id2)? id1: id2, (id1 < id2)? id2: id1);
    }

    private static String getPrivateKeyFile(int id) {
        return getPrefix(id) + PRIVATE_SUFFIX;
    }

    private static String getPublicKeyFile(int id) {
        return getPrefix(id) + PUBLIC_SUFFIX;
    }

    private static String getSecretKeyFile(int id1, int id2) {
        return getSecretPrefix(id1, id2) + SECRET_SUFFIX;
    }

    public static void generateKeyPair(int id) {
        generateKeyPair(getPrivateKeyFile(id), getPublicKeyFile(id));
    }

    public static void generateKeyFor(int id1, int id2) {
        generateSecretKey(getSecretKeyFile(id1, id2));
    }

    private static void generateKeyPair(String privatePathName, String publicPathName) {
        if (!Files.isDirectory(Path.of(".", KEYS_FOLDER), LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("Directory '" + KEYS_FOLDER + "' does not exist!");
        }
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
            throw new IllegalStateException(String.format("[ERROR] Generating key pair"));
        }
    }

    private static void generateSecretKey(String secretPathName) {
        if (!Files.isDirectory(Path.of(".", KEYS_FOLDER), LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("Directory '" + KEYS_FOLDER + "' does not exist!");
        }
        try {
            SecureRandom random;
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            random = SecureRandom.getInstance("SHA1PRNG");
            keyGen.init(128, random);
            SecretKey key = keyGen.generateKey();

            FileOutputStream secretFos = new FileOutputStream(secretPathName);
            secretFos.write(key.getEncoded());
            secretFos.close();

            keysFiles.add(new File(secretPathName));
        }
        catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException(String.format("[ERROR] Generating secret key"));
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
            e.printStackTrace();
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

    public static SecretKey getSecretKey(int id1, int id2) {
        SecretKey key = null;

        try {
            FileInputStream fis;
            fis = new FileInputStream(getSecretKeyFile(id1, id2));
            byte[] keyBytes = fis.readAllBytes();
            fis.close();

            key = new SecretKeySpec(keyBytes, 0, 16, "AES");
        } catch (Exception e) {
            throw new IllegalStateException(
                String.format("[ERROR] Getting secret key for processes %d and %d", id1, id2));
        }

        return key;
    }

    public static void cleanKeys() {
        for (File file : keysFiles) {
            file.delete();
        }
        keysFiles.clear();
    }
}
