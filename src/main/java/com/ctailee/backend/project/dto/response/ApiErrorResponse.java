package com.ctailee.backend.project.dto.response;

import java.util.Map;

public record ApiErrorResponse(
        String message,
        Map<String, String> errors
) {}
