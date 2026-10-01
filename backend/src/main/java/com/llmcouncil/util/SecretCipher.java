package com.llmcouncil.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

/**
 * Kullanıcının OpenRouter API key'ini DB'de şifreli saklamak için ince bir sarmalayıcı.
 * {@link Encryptors#delux} AES-256-GCM kullanır (her şifrelemede rastgele IV, authenticated) —
 * elle AES/GCM yazmak yerine bunu tercih ediyoruz çünkü IV/tag yönetimini doğru yapıyor.
 * Master key (secret+salt) DB'de DEĞİL, sadece ortam değişkeninden (.env) okunur.
 */
@Component
public class SecretCipher {

    private final TextEncryptor encryptor;

    public SecretCipher(@Value("${app.crypto.secret}") String secret,
                         @Value("${app.crypto.salt}") String salt) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "APP_CRYPTO_SECRET tanımlı değil — .env dosyasına ekleyin (openssl rand -base64 32).");
        }
        if (salt == null || salt.isBlank()) {
            throw new IllegalStateException(
                    "APP_CRYPTO_SALT tanımlı değil — .env dosyasına ekleyin (openssl rand -hex 16).");
        }
        this.encryptor = Encryptors.delux(secret, salt);
    }

    /** @return şifrelenmiş metin, ya da {@code null} girişi null/boşsa. */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return null;
        }
        return encryptor.encrypt(plaintext);
    }

    /** @return çözülmüş metin, ya da {@code null} girişi null/boşsa. */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return null;
        }
        return encryptor.decrypt(ciphertext);
    }
}
