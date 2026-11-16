package com.example.shortener.analytics;

import java.time.Instant;

public record ClickMessage(
        String code,
        Instant clickedAt,
        String userAgent,
        String referrer
) {}