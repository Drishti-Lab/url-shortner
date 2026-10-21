package com.example.urlshortener.controller;

import com.example.urlshortener.dto.ShortenRequest;
import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.dto.StatsResponse;
import com.example.urlshortener.model.UrlEntity;
import com.example.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
public class UrlController {

    private final UrlService service;

    public UrlController(UrlService service) {
        this.service = service;
    }

    @PostMapping("/api/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        UrlEntity saved = service.shorten(request.url());
        String shortUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/{code}")
                .buildAndExpand(saved.getCode())
                .toUriString();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ShortenResponse(saved.getCode(), shortUrl, saved.getOriginalUrl()));
    }

    @GetMapping("/api/stats/{code}")
    public StatsResponse stats(@PathVariable String code) {
        UrlEntity e = service.getStats(code);
        return new StatsResponse(e.getCode(), e.getOriginalUrl(), e.getClickCount(), e.getCreatedAt());
    }

    @GetMapping("/{code:[A-Za-z0-9]{4,16}}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        String target = service.resolve(code);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }
}
