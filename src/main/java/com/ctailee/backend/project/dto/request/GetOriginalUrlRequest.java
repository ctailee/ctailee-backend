package com.ctailee.backend.project.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GetOriginalUrlRequest(
        @NotBlank(message = "Short code can't be empty")
        String shortCode
) {
}
