package com.ctailee.backend.project.textcipher;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

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

            char replacement = encrypted.endsWith("A") ? 'B' : 'A';

            String modified = encrypted.substring(
                0,
                encrypted.length() - 1
            ) + replacement;

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