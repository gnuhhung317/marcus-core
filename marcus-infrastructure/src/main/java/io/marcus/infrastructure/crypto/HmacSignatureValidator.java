package io.marcus.infrastructure.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
public class HmacSignatureValidator {

    @Value("${marcus.security.master-key}")
    private String masterKey;

    public boolean isValid(String payload, String encryptedBotSecret, String providedSignature) {
        try {
            // 1. Decrypt the bot's secret using your masterKey
            String rawBotSecret = decryptSecret(encryptedBotSecret, masterKey);

            // 2. Calculate the HMAC-SHA256
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(rawBotSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            byte[] hashBytes = sha256_HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = bytesToHex(hashBytes); // Implement a simple hex converter

            // 3. USE TIMING-SAFE EQUALS! (Prevents Timing Attacks)
            return java.security.MessageDigest.isEqual(
                    calculatedSignature.getBytes(StandardCharsets.UTF_8),
                    providedSignature.getBytes(StandardCharsets.UTF_8)
            );

        } catch (Exception e) {
            // Log the error securely (don't print the secret!)
            return false;
        }
    }

    private String decryptSecret(String encrypted, String masterKey) {
        // Implement AES decryption here later.
        // For Phase 1 testing, just return the raw string.
        return encrypted;
    }


    // Ai-generated code for faster implementation :D
    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
