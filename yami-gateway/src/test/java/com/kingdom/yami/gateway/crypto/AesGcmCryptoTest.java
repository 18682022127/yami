package com.kingdom.yami.gateway.crypto;

import com.kingdom.yami.gateway.tools.AesGcmCrypto;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class AesGcmCryptoTest {

    @Test
    void testEncryptDecrypt() {
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);

        String plaintext = "{\"username\":\"test\",\"password\":\"secret123\"}";

        String encrypted = AesGcmCrypto.encrypt(plaintext, base64Key);
        assertThat(encrypted).isNotEmpty();
        assertThat(encrypted).isNotEqualTo(plaintext);

        String decrypted = AesGcmCrypto.decrypt(encrypted, base64Key);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void testEncryptionProducesDifferentCiphertext() {
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);

        String plaintext = "{\"data\":\"test\"}";

        String encrypted1 = AesGcmCrypto.encrypt(plaintext, base64Key);
        String encrypted2 = AesGcmCrypto.encrypt(plaintext, base64Key);

        assertThat(encrypted1).isNotEqualTo(encrypted2);

        assertThat(AesGcmCrypto.decrypt(encrypted1, base64Key)).isEqualTo(plaintext);
        assertThat(AesGcmCrypto.decrypt(encrypted2, base64Key)).isEqualTo(plaintext);
    }
}
