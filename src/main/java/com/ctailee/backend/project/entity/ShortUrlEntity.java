package com.ctailee.backend.project.entity;

import jakarta.persistence.*;
import org.hibernate.type.NumericBooleanConverter;

import java.time.LocalDateTime;

@Entity
@Table(name = "short_urls")
public class ShortUrlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(
            name = "original_url",
            nullable = false
    )
    private String originalUrl;

    @Column(
            name = "short_code",
            nullable = false,
            unique = true
    )
    private String shortCode;


    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "expires_at"
    )
    private LocalDateTime expiresAt;

    @Column(
            name = "is_active",
            nullable = false,
            insertable = false
    )
    @Convert(converter = NumericBooleanConverter.class)
    private boolean isActive = true;

    protected ShortUrlEntity(){}

    public ShortUrlEntity(String originalUrl, String shortCode, LocalDateTime expiresAt){
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.expiresAt = expiresAt;
    }

    public ShortUrlEntity(String originalUrl, String shortCode){
        this(originalUrl, shortCode, null);
    }

    public Integer getId(){
        return this.id;
    }

    public String getOriginalUrl(){
        return this.originalUrl;
    }

    public String getShortCode(){
        return this.shortCode;
    }

    public LocalDateTime getCreatedAt(){
        return this.createdAt;
    }

    public LocalDateTime getExpiresAt(){
        return this.expiresAt;
    }

    public boolean isActive(){ return this.isActive; }

    public void deactivate(){ this.isActive = false; }
}
