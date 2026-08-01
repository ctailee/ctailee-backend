package com.ctailee.backend.project.urlshortener;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;

@Component
public class UrlShortener implements ShortUrl {

    private static final int SHORT_CODE_LENGTH = 7;

    private static final String CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "abcdefghijklmnopqrstuvwxyz" +
            "0123456789";

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateShortCode() {
        StringBuilder shortCode = new StringBuilder(SHORT_CODE_LENGTH);

        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            int index = secureRandom.nextInt(CHARACTERS.length());
            shortCode.append(CHARACTERS.charAt(index));
        }

        return shortCode.toString();
    }
}
