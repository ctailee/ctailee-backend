package com.ctailee.backend.project.service;

import com.ctailee.textcipher.Pbkdf2AesGcmTextEncryptor;

public class TextCipherService {

    private final Pbkdf2AesGcmTextEncryptor textCipher;

    public TextCipherService(){
        textCipher = new Pbkdf2AesGcmTextEncryptor();
    }

    public String encrypt(String password, String plainText){
        String encryptedText = textCipher.encrypt(password.toCharArray(), plainText);

        return encryptedText;
    }

    public String decrypt(String password, String encryptedText){
        String plainText = textCipher.decrypt(password.toCharArray(), encryptedText);

        return plainText;
    }
}
