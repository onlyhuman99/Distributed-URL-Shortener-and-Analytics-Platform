package com.example.shortener.url;

public record CreateUrlResponse(
        String code,
        String shortUrl,
        String originalUrl
) {}