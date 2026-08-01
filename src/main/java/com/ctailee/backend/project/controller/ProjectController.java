package com.ctailee.backend.project.controller;

import com.ctailee.backend.project.dto.request.ShortenUrlRequest;
import com.ctailee.backend.project.dto.request.TextCipherDecryptRequest;
import com.ctailee.backend.project.dto.request.TextCipherEncryptRequest;
import com.ctailee.backend.project.dto.response.ShortenUrlResponse;
import com.ctailee.backend.project.dto.response.TextCipherDecryptResponse;
import com.ctailee.backend.project.dto.response.TextCipherEncryptResponse;
import com.ctailee.backend.project.service.ShortUrlService;
import com.ctailee.backend.project.service.TextCipherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/project")
public class ProjectController {

    private final TextCipherService textCipherService;
    private final ShortUrlService shortUrlService;

    public ProjectController(
            TextCipherService textCipherService,
            ShortUrlService shortUrlService
    ) {
        this.textCipherService = textCipherService;
        this.shortUrlService = shortUrlService;
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

    @PostMapping("/shorturl/shortenurl")
    public ShortenUrlResponse shortenUrl(
            @Valid @RequestBody ShortenUrlRequest request
    ) {
        String shortCode = this.shortUrlService.create(request.originalUrl()).getShortCode();

        return new ShortenUrlResponse(shortCode);
    }

    @GetMapping("/shorturl/{shortCode}")
    public ResponseEntity redirect(
            @PathVariable String shortCode
    ) {
        String originalUrl = this.shortUrlService.getOriginalUrl(shortCode);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }
}
