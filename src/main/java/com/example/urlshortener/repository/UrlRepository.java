package com.example.urlshortener.repository;

import com.example.urlshortener.model.UrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UrlRepository extends JpaRepository<UrlEntity, Long> {
    Optional<UrlEntity> findByCode(String code);
    boolean existsByCode(String code);
}
