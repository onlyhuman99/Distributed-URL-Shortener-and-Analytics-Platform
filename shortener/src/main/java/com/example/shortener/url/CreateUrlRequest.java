package com.example.shortener.url;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateUrlRequest(@NotBlank @Size(max = 2048) String url, Instant expiresAt) {}