package net.noerlol.cystolchat.common;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Convertor {

    public static Message decodeMessage(String message) throws Exception {
        String[] parts = message.trim().split("\\|");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid message format: " + message + "'");
        }
        MessageType type = MessageType.valueOf(parts[0]);
        User user = new User(parts[1]);
        String actualMessage = parts[2];

        return new Message(decrypt(actualMessage, getKey()), user, type);
    }

    public static String encodeMessage(Message message) throws Exception {
        return message.getType().name() + "|" + message.getUser().getUsername() + "|" + encrypt(message.getMessage(), getKey());
    }

    private static SecretKeySpec getKey() {
        String base64Key = "PdeUMbWEUSmUIB7EAFfJwQ==";
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static String encrypt(String strToEncrypt, SecretKeySpec secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private static String decrypt(String strToDecrypt, SecretKeySpec secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decodedBytes = Base64.getDecoder().decode(strToDecrypt);
        byte[] originalBytes = cipher.doFinal(decodedBytes);
        return new String(originalBytes, StandardCharsets.UTF_8);
    }
}
