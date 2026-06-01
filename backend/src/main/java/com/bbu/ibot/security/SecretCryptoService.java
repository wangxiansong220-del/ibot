package com.bbu.ibot.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class SecretCryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    @Value("${ibot.security.crypto-secret}")
    private String secret;

    private SecretKeySpec secretKeySpec;

    @PostConstruct
    public void init() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] key = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        this.secretKeySpec = new SecretKeySpec(key, 0, 16, "AES");
    }

    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] merged = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, merged, 0, iv.length);
            System.arraycopy(encrypted, 0, merged, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(merged);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to encrypt secret", exception);
        }
    }

    public String decrypt(String encryptedValue) {
        if (!StringUtils.hasText(encryptedValue)) {
            return null;
        }
        try {
            byte[] merged = Base64.getDecoder().decode(encryptedValue);
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[merged.length - IV_LENGTH];
            System.arraycopy(merged, 0, iv, 0, IV_LENGTH);
            System.arraycopy(merged, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to decrypt secret", exception);
        }
    }
}
