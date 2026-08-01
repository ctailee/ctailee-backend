package com.ctailee.backend.project.urlshortener;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UrlShortenerTest {
    private final ShortUrl urlShortener = new UrlShortener();

    @Test
    public void shouldOnlyContainsSevenCharacters(){
        String shortCode = this.urlShortener.generateShortCode();

        Assertions.assertNotNull(shortCode);
        Assertions.assertEquals(shortCode.length(), 7);
    }

    @Test
    public void shouldOnlyContainsAlphabetAndNumber(){
        String shortCode = this.urlShortener.generateShortCode();

        Assertions.assertTrue(
                shortCode.matches("[A-Za-z0-9]{7}"),
                "Illegal short code: " + shortCode
        );
    }
}
