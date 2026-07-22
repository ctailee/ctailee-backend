package com.ctailee.backend.project.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TextCipherDecryptRequest(
        @NotBlank(message = "password must not be blank")
        String password,

        @NotBlank(message = "encrypted text must not be blank")
        String encryptedText
) {}
