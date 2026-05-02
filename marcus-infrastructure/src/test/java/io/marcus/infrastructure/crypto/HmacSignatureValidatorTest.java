package io.marcus.infrastructure.crypto;

import io.marcus.domain.service.EncryptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HmacSignatureValidatorTest {

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private HmacSignatureValidator validator;

    @Test
    void shouldValidateWhenSignatureMatches() throws Exception {
        String payload = "1711900800000\\n{\"botId\":\"bot-1\",\"symbol\":\"BTCUSDT\"}";
        String encryptedSecret = "enc:secret";
        String rawSecret = "bot-secret-123";

        when(encryptionService.decrypt(encryptedSecret)).thenReturn(rawSecret);

        String signature = buildSignature(payload, rawSecret);

        assertThat(validator.isValid(payload, encryptedSecret, signature)).isTrue();
    }

    @Test
    void shouldValidateWhenProvidedSignatureIsUppercase() throws Exception {
        String payload = "1711900800000\\n{\"botId\":\"bot-1\",\"symbol\":\"BTCUSDT\"}";
        String encryptedSecret = "enc:secret";
        String rawSecret = "bot-secret-123";

        when(encryptionService.decrypt(encryptedSecret)).thenReturn(rawSecret);

        String uppercaseSignature = buildSignature(payload, rawSecret).toUpperCase(Locale.ROOT);

        assertThat(validator.isValid(payload, encryptedSecret, uppercaseSignature)).isTrue();
    }

    @Test
    void shouldRejectWhenSignatureDoesNotMatch() {
        String payload = "1711900800000\\n{\"botId\":\"bot-1\",\"symbol\":\"BTCUSDT\"}";
        String encryptedSecret = "enc:secret";

        when(encryptionService.decrypt(encryptedSecret)).thenReturn("bot-secret-123");

        assertThat(validator.isValid(payload, encryptedSecret, "bad-signature")).isFalse();
    }

    @Test
    void shouldRejectWhenDecryptFails() {
        when(encryptionService.decrypt(anyString())).thenThrow(new IllegalStateException("decrypt failed"));

        assertThat(validator.isValid("payload", "enc:secret", "signature")).isFalse();
    }

    private String buildSignature(String payload, String rawSecret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(rawSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
