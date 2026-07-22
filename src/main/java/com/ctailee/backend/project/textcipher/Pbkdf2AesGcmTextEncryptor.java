package com.ctailee.backend.project.textcipher;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public final class Pbkdf2AesGcmTextEncryptor implements TextCipher {

    private static final String FORMAT_VERSION = "pte1"; // pte = password text encryption
    private static final String KDF_ID = "pbkdf2-sha256";
    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int DEFAULT_ITERATIONS = 600_000;
    private static final int MAX_ACCEPTED_ITERATIONS = 10_000_000;
    private static final int KEY_LENGTH_BITS = 256;

    /*
     * 128-bit random salt.
     */
    private static final int SALT_LENGTH_BYTES = 16;

    /*
     * GCM standard IV size: 96 bits.
     */
    private static final int IV_LENGTH_BYTES = 12;

    private static final int TAG_LENGTH_BITS = 128;
    private static final int TAG_LENGTH_BYTES = TAG_LENGTH_BITS / Byte.SIZE;

    private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_DECODER = Base64.getUrlDecoder();

    private final SecureRandom secureRandom;

    public Pbkdf2AesGcmTextEncryptor() {
        secureRandom = new SecureRandom();
    }

    @Override
    public String encrypt(char[] password, String plaintext) {
        validatePassword(password);
        Objects.requireNonNull(
            plaintext,
            "plaintext must not be null"
        );

        byte[] salt = randomBytes(SALT_LENGTH_BYTES);
        byte[] iv = randomBytes(IV_LENGTH_BYTES);

        String header = createHeader(DEFAULT_ITERATIONS);

        SecretKey encryptionKey = deriveKey(
            password,
            salt,
            DEFAULT_ITERATIONS
        );

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);

            GCMParameterSpec parameters = new GCMParameterSpec(TAG_LENGTH_BITS, iv);

            cipher.init(
                Cipher.ENCRYPT_MODE,
                encryptionKey,
                parameters
            );

            /*
             * Authenticate the format, KDF name, and iteration count.
             * AAD is verified but not encrypted.
             */
            cipher.updateAAD(
                header.getBytes(StandardCharsets.US_ASCII)
            );

            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);

            /*
             * For GCM, the returned byte array contains:
             *
             * ciphertext + authentication tag
             */
            byte[] encryptedBytes = cipher.doFinal(plaintextBytes);

            return String.join(
                ".",
                FORMAT_VERSION,
                KDF_ID,
                Integer.toString(DEFAULT_ITERATIONS),
                encode(salt),
                encode(iv),
                encode(encryptedBytes)
            );
        } catch (GeneralSecurityException exception) {
            throw new EncryptionException(
                "Unable to encrypt the supplied text.",
                exception
            );
        }
    }

    @Override
    public String decrypt(
        char[] password,
        String encryptedToken
    ) {
        validatePassword(password);
        Objects.requireNonNull(
            encryptedToken,
            "encryptedToken must not be null"
        );

        EncryptedPayload payload = parseToken(encryptedToken);

        SecretKey encryptionKey = deriveKey(
            password,
            payload.salt(),
            payload.iterations()
        );

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);

            GCMParameterSpec parameters = new GCMParameterSpec(
                    TAG_LENGTH_BITS,
                    payload.iv()
                );

            cipher.init(
                Cipher.DECRYPT_MODE,
                encryptionKey,
                parameters
            );

            cipher.updateAAD(
                payload.header().getBytes(StandardCharsets.US_ASCII)
            );

            byte[] plaintextBytes =
                cipher.doFinal(payload.ciphertext());

            return new String(
                plaintextBytes,
                StandardCharsets.UTF_8
            );
        } catch (AEADBadTagException exception) {
            /*
             * Do not reveal whether:
             *
             * 1. The password was wrong;
             * 2. The token was modified;
             * 3. The ciphertext was damaged.
             */
            throw new EncryptionException(
                "Unable to decrypt the token. "
                + "The password is incorrect or "
                + "the encrypted content was modified.",
                exception
            );
        } catch (GeneralSecurityException exception) {
            throw new EncryptionException(
                "Unable to decrypt the supplied token.",
                exception
            );
        }
    }

    private SecretKey deriveKey(
        char[] password,
        byte[] salt,
        int iterations
    ) {
        /*
         * PBEKeySpec creates an internal password copy.
         */
        PBEKeySpec keySpec = new PBEKeySpec(
            password,
            salt,
            iterations,
            KEY_LENGTH_BITS
        );

        byte[] derivedKeyBytes = null;

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
            derivedKeyBytes = factory.generateSecret(keySpec).getEncoded();

            /*
             * SecretKeySpec copies the supplied key bytes.
             */
            return new SecretKeySpec(
                derivedKeyBytes,
                "AES"
            );
        } catch (GeneralSecurityException exception) {
            throw new EncryptionException(
                "Unable to derive an encryption key.",
                exception
            );
        } finally {
            keySpec.clearPassword();

            if (derivedKeyBytes != null) {
                Arrays.fill(derivedKeyBytes, (byte) 0);
            }
        }
    }

    private EncryptedPayload parseToken(String token) {
        String[] parts = token.split("\\.", -1);

        if (parts.length != 6) {
            throw new EncryptionException(
                "Invalid encrypted token format."
            );
        }

        if (!FORMAT_VERSION.equals(parts[0])) {
            throw new EncryptionException(
                "Unsupported encrypted token version."
            );
        }

        if (!KDF_ID.equals(parts[1])) {
            throw new EncryptionException(
                "Unsupported key derivation algorithm."
            );
        }

        int iterations = parseIterations(parts[2]);

        byte[] salt = decode(parts[3], "salt");
        byte[] iv = decode(parts[4], "IV");
        byte[] ciphertext = decode(parts[5], "ciphertext");

        if (salt.length != SALT_LENGTH_BYTES) {
            throw new EncryptionException(
                "Invalid salt length."
            );
        }

        if (iv.length != IV_LENGTH_BYTES) {
            throw new EncryptionException(
                "Invalid IV length."
            );
        }

        /*
         * Even empty plaintext produces an authentication tag.
         */
        if (ciphertext.length < TAG_LENGTH_BYTES) {
            throw new EncryptionException(
                "Invalid encrypted payload."
            );
        }

        String header = String.join(
            ".",
            parts[0],
            parts[1],
            parts[2]
        );

        return new EncryptedPayload(
            header,
            iterations,
            salt,
            iv,
            ciphertext
        );
    }

    private static int parseIterations(String value) {
        final int iterations;

        try {
            iterations = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new EncryptionException(
                "Invalid PBKDF2 iteration count.",
                exception
            );
        }

        /*
         * The upper bound prevents malicious tokens from forcing
         * extremely expensive key derivations.
         */
        if (iterations <= 0 || iterations > MAX_ACCEPTED_ITERATIONS) {
            throw new EncryptionException(
                "Unsupported PBKDF2 iteration count."
            );
        }

        return iterations;
    }

    private static String createHeader(int iterations) {
        return String.join(
            ".",
            FORMAT_VERSION,
            KDF_ID,
            Integer.toString(iterations)
        );
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    private static String encode(byte[] bytes) {
        return BASE64_ENCODER.encodeToString(bytes);
    }

    private static byte[] decode(
            String value,
            String fieldName
    ) {
        try {
            return BASE64_DECODER.decode(value);
        } catch (IllegalArgumentException exception) {
            throw new EncryptionException(
                "Invalid Base64 data in " + fieldName + ".",
                exception
            );
        }
    }

    private static void validatePassword(char[] password) {
        Objects.requireNonNull(
            password,
            "password must not be null"
        );

        if (password.length == 0) {
            throw new IllegalArgumentException(
                "password must not be empty"
            );
        }
    }

    private record EncryptedPayload(
        String header,
        int iterations,
        byte[] salt,
        byte[] iv,
        byte[] ciphertext
    ) {}
}