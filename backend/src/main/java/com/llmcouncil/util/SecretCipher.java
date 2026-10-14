package com.llmcouncil.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper for storing the user's OpenRouter API key encrypted in the DB.
 * {@link Encryptors#delux} uses AES-256-GCM (random IV per encryption, authenticated) — we
 * prefer it over hand-rolling AES/GCM because it handles IV/tag management correctly.
 * The master key (secret+salt) is read only from an environment variable (.env), NOT from the DB.
 */
@Component
public class SecretCipher {

    private final TextEncryptor encryptor;

    public SecretCipher(@Value("${app.crypto.secret}") String secret,
                         @Value("${app.crypto.salt}") String salt) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "APP_CRYPTO_SECRET is not set — add it to your .env file (openssl rand -base64 32).");
        }
        if (salt == null || salt.isBlank()) {
            throw new IllegalStateException(
                    "APP_CRYPTO_SALT is not set — add it to your .env file (openssl rand -hex 16).");
        }
        this.encryptor = Encryptors.delux(secret, salt);
    }

    /** @return the encrypted text, or {@code null} if the input is null/blank. */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return null;
        }
        return encryptor.encrypt(plaintext);
    }

    /** @return the decrypted text, or {@code null} if the input is null/blank. */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return null;
        }
        return encryptor.decrypt(ciphertext);
    }
}
