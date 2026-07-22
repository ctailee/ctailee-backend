package com.ctailee.backend.project.textcipher;

public interface TextCipher {

    /**
     * Uses the supplied password to encrypt plaintext.
     *
     * @param password  user password; must not be null or empty
     * @param plaintext plaintext; must not be null
     * @return versioned encrypted token
     */
    String encrypt(char[] password, String plaintext);

    /**
     * Uses the supplied password to decrypt an encrypted token.
     *
     * @param password       user password
     * @param encryptedToken token returned by encrypt
     * @return original plaintext
     */
    String decrypt(char[] password, String encryptedToken);
}