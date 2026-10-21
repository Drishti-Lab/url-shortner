package com.example.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;

public record ShortenRequest(@NotBlank(message = "url must not be blank") String url) {
}
