package com.example.shortener.url;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class UrlService {

    private static final long ID_OFFSET = 100_000_000L;

    private final ShortUrlRepository repo;
    private final StringRedisTemplate redis;

    @Value("${app.cache.ttl-hours}")
    private long ttlHours;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional
    public CreateUrlResponse create(CreateUrlRequest req, Long ownerId) {

        URI uri = URI.create(req.url());

        if (uri.getHost() == null ||
                !("http".equals(uri.getScheme()) ||
                  "https".equals(uri.getScheme()))) {

            throw new IllegalArgumentException(
                    "Only absolute http/https URLs are allowed");
        }

        ShortUrl e = new ShortUrl();

        e.setOriginalUrl(req.url());
        e.setOwnerId(ownerId);
        e.setExpiresAt(req.expiresAt());

        repo.saveAndFlush(e);

        e.setCode(Base62.encode(e.getId() + ID_OFFSET));

        return new CreateUrlResponse(
                e.getCode(),
                baseUrl + "/" + e.getCode(),
                e.getOriginalUrl()
        );
    }

    public void assertOwner(String code, Long userId) {

        ShortUrl s = repo.findByCode(code)
                .orElseThrow(() -> new NotFoundException(code));

        if (!s.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    // resolve() will be implemented in Section 7.
}