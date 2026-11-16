package com.example.shortener.analytics;

import com.example.shortener.url.UrlService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class StatsController {

    private final UrlService urls;
    private final ClickEventRepository clicks;

    record DayCount(String day, long clicks) {}

    @GetMapping("/api/urls/{code}/stats")
    public Map<String, Object> stats(
            @PathVariable String code,
            @AuthenticationPrincipal Long userId) {

        urls.assertOwner(code, userId);

        var daily = clicks.dailyClicks(code)
                .stream()
                .map(d -> new DayCount(d.getDay(), d.getClicks()))
                .toList();

        return Map.of(
                "code", code,
                "totalClicks", clicks.countByCode(code),
                "daily", daily
        );
    }
}