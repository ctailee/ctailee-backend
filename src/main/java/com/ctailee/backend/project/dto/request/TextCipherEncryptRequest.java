package com.ctailee.backend.project.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TextCipherEncryptRequest(
        @NotBlank(message = "password must not be blank")
        String password,

        @NotBlank(message = "plain text must not be blank")
        String plainText
) {}
