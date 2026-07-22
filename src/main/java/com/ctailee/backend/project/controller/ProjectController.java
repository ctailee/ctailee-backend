package com.ctailee.backend.project.controller;

import com.ctailee.backend.project.dto.request.TextCipherDecryptRequest;
import com.ctailee.backend.project.dto.request.TextCipherEncryptRequest;
import com.ctailee.backend.project.dto.response.TextCipherDecryptResponse;
import com.ctailee.backend.project.dto.response.TextCipherEncryptResponse;
import com.ctailee.backend.project.service.TextCipherService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/project")
public class ProjectController {

    private final TextCipherService textCipherService;

    public ProjectController() {
        textCipherService = new TextCipherService();
    }

    @PostMapping("/textcipher/encrypt")
    public TextCipherEncryptResponse textCipherEncrypt(
            @Valid @RequestBody TextCipherEncryptRequest request
    ) {
        String encryptedText = textCipherService.encrypt(
                request.password(),
                request.plainText()
        );

        return new TextCipherEncryptResponse(encryptedText);
    }

    @PostMapping("/textcipher/decrypt")
    public TextCipherDecryptResponse textCipherDecrypt(
            @Valid @RequestBody TextCipherDecryptRequest request
    ) {
        String plainText = textCipherService.decrypt(
                request.password(),
                request.encryptedText()
        );

        return new TextCipherDecryptResponse(plainText);
    }
}
