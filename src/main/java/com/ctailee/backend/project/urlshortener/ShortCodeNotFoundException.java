package com.ctailee.backend.project.urlshortener;

public final class ShortCodeNotFoundException extends RuntimeException {
    public ShortCodeNotFoundException(String message){
        super("Short code not found: " + message);
    }

    public ShortCodeNotFoundException(String message, Throwable cause){
        super("Short code not found: " + message, cause);
    }
}
