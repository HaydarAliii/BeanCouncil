package com.llmcouncil.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretCipherTest {

    private static final String SECRET = "test-master-secret-value";
    private static final String SALT = "abcdef0123456789"; // Encryptors.delux hex salt bekler

    @Test
    void blankSecret_throwsIllegalState() {
        assertThatThrownBy(() -> new SecretCipher("", SALT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_CRYPTO_SECRET");
    }

    @Test
    void nullSecret_throwsIllegalState() {
        assertThatThrownBy(() -> new SecretCipher(null, SALT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void blankSalt_throwsIllegalState() {
        assertThatThrownBy(() -> new SecretCipher(SECRET, ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_CRYPTO_SALT");
    }

    @Test
    void encrypt_nullOrBlankInput_returnsNull() {
        SecretCipher cipher = new SecretCipher(SECRET, SALT);
        assertThat(cipher.encrypt(null)).isNull();
        assertThat(cipher.encrypt("")).isNull();
        assertThat(cipher.encrypt("   ")).isNull();
    }

    @Test
    void decrypt_nullOrBlankInput_returnsNull() {
        SecretCipher cipher = new SecretCipher(SECRET, SALT);
        assertThat(cipher.decrypt(null)).isNull();
        assertThat(cipher.decrypt("")).isNull();
    }

    @Test
    void encryptThenDecrypt_roundTripsToOriginal() {
        SecretCipher cipher = new SecretCipher(SECRET, SALT);
        String plaintext = "sk-or-v1-gerçek-bir-key-degil-ama-ayni-sekilde-uzun";

        String ciphertext = cipher.encrypt(plaintext);

        assertThat(ciphertext).isNotNull().isNotEqualTo(plaintext);
        assertThat(cipher.decrypt(ciphertext)).isEqualTo(plaintext);
    }

    @Test
    void encrypt_sameInputTwice_producesDifferentCiphertext() {
        // Encryptors.delux her şifrelemede rastgele bir IV kullanır — aynı girdi için bile
        // çıktı her seferinde farklı olmalı (authenticated encryption'ın beklenen özelliği).
        SecretCipher cipher = new SecretCipher(SECRET, SALT);
        String plaintext = "ayni-girdi";

        String first = cipher.encrypt(plaintext);
        String second = cipher.encrypt(plaintext);

        assertThat(first).isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo(plaintext);
        assertThat(cipher.decrypt(second)).isEqualTo(plaintext);
    }
}
