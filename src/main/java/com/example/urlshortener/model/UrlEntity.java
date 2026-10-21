package com.example.urlshortener.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "urls")
public class UrlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "click_count", nullable = false)
    private long clickCount = 0;

    protected UrlEntity() {
        // required by JPA
    }

    public UrlEntity(String code, String originalUrl) {
        this.code = code;
        this.originalUrl = originalUrl;
    }

    public void incrementClicks() {
        this.clickCount++;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getOriginalUrl() { return originalUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public long getClickCount() { return clickCount; }
}
