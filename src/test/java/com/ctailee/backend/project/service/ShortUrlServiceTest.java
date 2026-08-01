package com.ctailee.backend.project.service;

import com.ctailee.backend.project.entity.ShortUrlEntity;
import com.ctailee.backend.project.repository.ShortUrlRepository;
import com.ctailee.backend.project.urlshortener.ShortCodeNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/short-url-service-test.sqlite",
        "spring.jpa.show-sql=false"
})
class ShortUrlServiceTest {

    @Autowired
    private ShortUrlService service;

    @Autowired
    private ShortUrlRepository repository;

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
    }

    @Test
    void getOriginalUrl_returnsOriginalUrl_whenShortUrlIsActiveAndNotExpired() {
        ShortUrlEntity entity = repository.save(new ShortUrlEntity(
                "https://ctailee.com/",
                "active1",
                LocalDateTime.now().plusDays(1)
        ));

        assertTrue(entity.isActive());
        assertEquals("https://ctailee.com/", service.getOriginalUrl("active1"));
    }

    @Test
    void getOriginalUrl_returnsOriginalUrl_whenExpirationIsNotSet() {
        repository.save(new ShortUrlEntity(
                "https://ctailee.com/no-expiration",
                "noexpire"
        ));

        assertEquals(
                "https://ctailee.com/no-expiration",
                service.getOriginalUrl("noexpire")
        );
    }

    @Test
    void getOriginalUrl_deactivatesExpiredShortUrl_evenWhenExceptionIsThrown() {
        repository.save(new ShortUrlEntity(
                "https://ctailee.com/expired",
                "expired1",
                LocalDateTime.now().minusMinutes(1)
        ));

        assertThrows(
                ShortCodeNotFoundException.class,
                () -> service.getOriginalUrl("expired1")
        );

        ShortUrlEntity persistedEntity = repository.findByShortCode("expired1")
                .orElseThrow();
        assertFalse(persistedEntity.isActive());
    }

    @Test
    void getOriginalUrl_throwsException_whenShortUrlIsInactive() {
        ShortUrlEntity entity = repository.save(new ShortUrlEntity(
                "https://ctailee.com/inactive",
                "inactive1",
                LocalDateTime.now().plusDays(1)
        ));
        entity.deactivate();
        repository.save(entity);

        assertThrows(
                ShortCodeNotFoundException.class,
                () -> service.getOriginalUrl("inactive1")
        );
    }

    @Test
    void getOriginalUrl_throwsException_whenShortCodeDoesNotExist() {
        assertThrows(
                ShortCodeNotFoundException.class,
                () -> service.getOriginalUrl("missing")
        );
    }
}
