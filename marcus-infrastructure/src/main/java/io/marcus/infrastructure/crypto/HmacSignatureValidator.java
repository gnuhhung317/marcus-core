package io.marcus.infrastructure.crypto;

import io.marcus.domain.service.EncryptionService;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class HmacSignatureValidator {

    private final EncryptionService encryptionService;
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    public HmacSignatureValidator(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public boolean isValid(String payload, String encryptedBotSecret, String providedSignature) {
        if (payload == null || encryptedBotSecret == null || providedSignature == null) {
            return false;
        }

        try {
            String rawBotSecret = encryptionService.decrypt(encryptedBotSecret);

            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(rawBotSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            byte[] hashBytes = sha256_HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = HEX_FORMAT.formatHex(hashBytes);
            String normalizedProvidedSignature = providedSignature.toLowerCase(Locale.ROOT);

            return java.security.MessageDigest.isEqual(
                    calculatedSignature.getBytes(StandardCharsets.UTF_8),
                    normalizedProvidedSignature.getBytes(StandardCharsets.UTF_8)
            );

        } catch (GeneralSecurityException | RuntimeException e) {
            return false;
        }
    }
}
