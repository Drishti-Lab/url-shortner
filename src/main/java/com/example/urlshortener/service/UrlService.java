package com.example.urlshortener.service;

import com.example.urlshortener.exception.InvalidUrlException;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.model.UrlEntity;
import com.example.urlshortener.repository.UrlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;

@Service
public class UrlService {

    static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    static final int CODE_LENGTH = 7;
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_URL_LENGTH = 2048;

    private final UrlRepository repository;
    private final SecureRandom random = new SecureRandom();

    public UrlService(UrlRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UrlEntity shorten(String originalUrl) {
        String url = validate(originalUrl);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = generateCode();
            if (!repository.existsByCode(code)) {
                return repository.save(new UrlEntity(code, url));
            }
        }
        throw new IllegalStateException("Could not generate a unique short code");
    }

    /** Returns the original URL for a code and records the click. */
    @Transactional
    public String resolve(String code) {
        UrlEntity entity = repository.findByCode(code)
                .orElseThrow(() -> new UrlNotFoundException(code));
        entity.incrementClicks();
        repository.save(entity);
        return entity.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public UrlEntity getStats(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new UrlNotFoundException(code));
    }

    String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private String validate(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new InvalidUrlException("URL must not be empty");
        }
        String url = originalUrl.trim();
        if (url.length() > MAX_URL_LENGTH) {
            throw new InvalidUrlException("URL is too long");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("URL is not valid");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new InvalidUrlException("URL must start with http:// or https://");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new InvalidUrlException("URL must contain a host");
        }
        return url;
    }
}
