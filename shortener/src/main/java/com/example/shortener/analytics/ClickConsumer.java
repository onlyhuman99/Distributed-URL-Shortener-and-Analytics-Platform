package com.example.shortener.analytics;

import lombok.RequiredArgsConstructor;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClickConsumer {

    private final ClickEventRepository repo;

    @KafkaListener(topics = ClickPublisher.TOPIC)
    public void onClick(ClickMessage m) {

        ClickEvent e = new ClickEvent();

        e.setCode(m.code());
        e.setClickedAt(m.clickedAt());
        e.setUserAgent(cut(m.userAgent(), 512));
        e.setReferrer(cut(m.referrer(), 2048));

        repo.save(e);
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max
                ? s
                : s.substring(0, max);
    }
}