package com.example.shortener.url;

import com.example.shortener.analytics.ClickPublisher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;
    private final ClickPublisher clickPublisher;

    @PostMapping("/api/urls")
    public ResponseEntity<CreateUrlResponse> create(
            @Valid @RequestBody CreateUrlRequest req,
            @AuthenticationPrincipal Long userId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(urlService.create(req, userId));
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(
            @PathVariable String code,
            HttpServletRequest request) {

        String target = urlService.resolve(code);

        clickPublisher.publish(code, request);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(target))
                .build();
    }
}