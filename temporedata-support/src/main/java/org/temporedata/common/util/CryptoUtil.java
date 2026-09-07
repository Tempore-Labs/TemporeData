package org.temporedata.common.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Simple AES crypto for datasource password encryption.
 */
public final class CryptoUtil {

    private static final String ALGORITHM = "AES";

    private CryptoUtil() {
    }

    public static String encrypt(String plain, String key) {
        try {
            SecretKeySpec spec = new SecretKeySpec(padKey(key), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, spec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("AES encrypt failed", e);
        }
    }

    public static String decrypt(String encrypted, String key) {
        try {
            SecretKeySpec spec = new SecretKeySpec(padKey(key), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, spec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decrypt failed", e);
        }
    }

    private static byte[] padKey(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] padded = new byte[16]; // AES-128
        System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 16));
        return padded;
    }
}