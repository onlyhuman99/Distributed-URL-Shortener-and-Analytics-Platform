package com.example.shortener.url;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.shortener.common.NotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock ShortUrlRepository repo;
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> ops;
    @InjectMocks UrlService service;

    @BeforeEach
    void setUp() {
        // @Value fields are not set when Spring is not running, so set them by hand
        ReflectionTestUtils.setField(service, "ttlHours", 24L);
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080");
    }

    @Test
    void cacheHitSkipsDatabase() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("url:abc")).thenReturn("https://example.com");

        assertEquals("https://example.com", service.resolve("abc"));
        verifyNoInteractions(repo);                       // the whole point of the cache
    }

    @Test
    void cacheMissLoadsFromDatabaseAndFillsCache() {
        ShortUrl row = new ShortUrl();
        row.setOriginalUrl("https://example.com");
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("url:abc")).thenReturn(null);
        when(repo.findByCode("abc")).thenReturn(Optional.of(row));

        assertEquals("https://example.com", service.resolve("abc"));
        verify(ops).set(eq("url:abc"), eq("https://example.com"), any(Duration.class));
    }

    @Test
    void unknownCodeThrowsNotFound() {
        when(redis.opsForValue()).thenReturn(ops);
        when(repo.findByCode("nope")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.resolve("nope"));
    }

    @Test
    void expiredLinkThrowsNotFoundAndIsNotCached() {
        ShortUrl row = new ShortUrl();
        row.setOriginalUrl("https://example.com");
        row.setExpiresAt(Instant.now().minusSeconds(60));
        when(redis.opsForValue()).thenReturn(ops);
        when(repo.findByCode("old")).thenReturn(Optional.of(row));

        assertThrows(NotFoundException.class, () -> service.resolve("old"));
        verify(ops, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void rejectsNonHttpUrls() {
        assertThrows(IllegalArgumentException.class,
            () -> service.create(new CreateUrlRequest("javascript:alert(1)", null), 1L));
        verifyNoInteractions(repo);                       // nothing must reach the database
    }

    @Test
    void createBuildsCodeFromDatabaseId() {
        when(repo.saveAndFlush(any(ShortUrl.class))).thenAnswer(inv -> {
            ShortUrl saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);   // pretend the DB assigned id 1
            return saved;
        });

        CreateUrlResponse res = service.create(new CreateUrlRequest("https://example.com", null), 7L);

        String expected = Base62.encode(100_000_001L);        // id 1 + the offset
        assertEquals(expected, res.code());
        assertEquals("http://localhost:8080/" + expected, res.shortUrl());
    }
}