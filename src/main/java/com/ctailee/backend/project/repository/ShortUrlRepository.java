package com.ctailee.backend.project.repository;

import com.ctailee.backend.project.entity.ShortUrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShortUrlRepository extends JpaRepository<ShortUrlEntity, Long> {
    Optional<ShortUrlEntity> findByShortCode(String shortCode);
    boolean existsByShortCode(String shortCode);
}
