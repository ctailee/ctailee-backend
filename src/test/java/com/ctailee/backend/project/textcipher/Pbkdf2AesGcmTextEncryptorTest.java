package com.ctailee.backend.project.textcipher;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class Pbkdf2AesGcmTextEncryptorTest {

    private final TextCipher encryptor = new Pbkdf2AesGcmTextEncryptor();

    @Test
    void shouldEncryptAndDecryptText() {
        char[] password = "I'm a password!!!".toCharArray();

        try {
            String encrypted = encryptor.encrypt(
                password,
                "中文、English、Emoji 🔐"
            );

            String decrypted = encryptor.decrypt(
                password,
                encrypted
            );

            assertEquals(
                "中文、English、Emoji 🔐",
                decrypted
            );
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    @Test
    void samePasswordAndTextShouldProduceDifferentTokens() {
        char[] password = "correct horse battery staple".toCharArray();

        try {
            String first = encryptor.encrypt(password, "secret");
            String second = encryptor.encrypt(password, "secret");

            assertNotEquals(first, second);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    @Test
    void wrongPasswordShouldFail() {
        char[] correctPassword = "correct password".toCharArray();

        char[] wrongPassword = "wrong password".toCharArray();

        try {
            String encrypted = encryptor.encrypt(
                correctPassword,
                "secret"
            );

            assertThrows(
                EncryptionException.class,
                () -> encryptor.decrypt(
                    wrongPassword,
                    encrypted
                )

            );
        } finally {
            Arrays.fill(correctPassword, '\0');
            Arrays.fill(wrongPassword, '\0');
        }
    }

    @Test
    void modifiedTokenShouldFail() {
        char[] password = "correct password".toCharArray();

        try {
            String encrypted = encryptor.encrypt(password, "secret");
            String[] parts = encrypted.split("\\.", -1);

            byte[] ciphertext = Base64.getUrlDecoder().decode(parts[5]);
            ciphertext[ciphertext.length - 1] ^= 0x01;

            parts[5] = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(ciphertext);

            String modified = String.join(".", parts);

            assertThrows(
                EncryptionException.class,
                () -> encryptor.decrypt(
                    password,
                    modified
                )
            );
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
