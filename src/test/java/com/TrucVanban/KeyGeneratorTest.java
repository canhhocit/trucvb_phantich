package com.TrucVanban;

import org.junit.jupiter.api.Test;
import java.io.FileWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Base64;

public class KeyGeneratorTest {

    @Test
    public void generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyPairGen.generateKeyPair();

        // Format Private Key as PKCS#8 PEM
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
        String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" +
                insertNewLines(Base64.getEncoder().encodeToString(privateKeyBytes)) +
                "\n-----END PRIVATE KEY-----";

        // Format Public Key as X.509 PEM
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" +
                insertNewLines(Base64.getEncoder().encodeToString(publicKeyBytes)) +
                "\n-----END PUBLIC KEY-----";

        // Write to files in project root
        try (FileWriter privateKeyWriter = new FileWriter("private_key.pem")) {
            privateKeyWriter.write(privateKeyPem);
        }
        try (FileWriter publicKeyWriter = new FileWriter("public_key.pem")) {
            publicKeyWriter.write(publicKeyPem);
        }

        System.out.println("==================================================");
        System.out.println("GENERATED RSA 2048-BIT KEYPAIR SUCCESSFULLY!");
        System.out.println("Files created in project root:");
        System.out.println("- private_key.pem");
        System.out.println("- public_key.pem");
        System.out.println("==================================================");
        System.out.println("PUBLIC KEY PEM CONTENT (use this in certificates table):");
        System.out.println(publicKeyPem);
        System.out.println("==================================================");
    }

    private String insertNewLines(String base64) {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        while (index < base64.length()) {
            sb.append(base64, index, Math.min(index + 64, base64.length()));
            sb.append("\n");
            index += 64;
        }
        return sb.toString().trim();
    }
}
