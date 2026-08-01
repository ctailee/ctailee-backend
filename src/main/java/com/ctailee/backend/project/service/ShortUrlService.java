package com.ctailee.backend.project.service;

import com.ctailee.backend.project.entity.ShortUrlEntity;
import com.ctailee.backend.project.repository.ShortUrlRepository;
import com.ctailee.backend.project.urlshortener.ShortCodeNotFoundException;
import com.ctailee.backend.project.urlshortener.ShortUrl;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Transactional
public class ShortUrlService {
    private final ShortUrlRepository repository;
    private final ShortUrl urlShortener;

    public ShortUrlService(ShortUrlRepository repository, ShortUrl urlShortener){
        this.repository = repository;
        this.urlShortener = urlShortener;
    }

    public ShortUrlEntity create(String originalUrl){
        String shortCode;
        do{
            shortCode = this.urlShortener.generateShortCode();
        }while(this.repository.existsByShortCode(shortCode));

        ShortUrlEntity entity = new ShortUrlEntity(
                originalUrl,
                shortCode,
                LocalDateTime.now().plusDays(3)
        );

        this.repository.save(entity);
        return entity;
    }

    public String getOriginalUrl(String shortCode){
        ShortUrlEntity entity = this.repository.findByShortCode(shortCode).orElseThrow(
                () -> new ShortCodeNotFoundException(shortCode)
        );

        return entity.getOriginalUrl();
    }
}