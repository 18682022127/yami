package com.kingdom.yami.gateway.tools;

import com.kingdom.yami.common.exception.CryptoException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class AesGcmCrypto {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String encrypt(String plaintext, String base64Key, byte[] aad) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            SecretKey key = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            if (aad != null && aad.length > 0) {
                cipher.updateAAD(aad);
            }

            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintextBytes);

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new CryptoException("Encryption failed", e);
        }
    }

    public static String encrypt(String plaintext, String base64Key) {
        return encrypt(plaintext, base64Key, null);
    }

    public static String decrypt(String encryptedData, String base64Key, byte[] aad) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            SecretKey key = new SecretKeySpec(keyBytes, "AES");

            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);

            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            if (aad != null && aad.length > 0) {
                cipher.updateAAD(aad);
            }

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CryptoException("Decryption failed", e);
        }
    }

    public static String decrypt(String encryptedData, String base64Key) {
        return decrypt(encryptedData, base64Key, null);
    }
}
