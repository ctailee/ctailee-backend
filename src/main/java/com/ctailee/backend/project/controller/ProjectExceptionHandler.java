package com.ctailee.backend.project.controller;

import com.ctailee.backend.project.dto.response.ApiErrorResponse;
import com.ctailee.textcipher.EncryptionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = ProjectController.class)
public class ProjectExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity.badRequest().body(
                new ApiErrorResponse("Request validation failed", errors)
        );
    }

    @ExceptionHandler(EncryptionException.class)
    public ResponseEntity<ApiErrorResponse> handleEncryptionException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiErrorResponse(
                        "Unable to process the encrypted text",
                        Map.of()
                )
        );
    }
}
