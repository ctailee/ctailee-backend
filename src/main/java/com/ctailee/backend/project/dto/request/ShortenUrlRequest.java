package com.ctailee.backend.project.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ShortenUrlRequest(
        @NotBlank(message = "Original URL can't be empty")
        String originalUrl
) {
}
