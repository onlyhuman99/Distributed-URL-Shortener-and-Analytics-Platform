package com.example.shortener.analytics;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClickPublisher {

    public static final String TOPIC = "click-events";

    private final KafkaTemplate<String, ClickMessage> kafka;

    public void publish(String code, HttpServletRequest req) {

        try {
            kafka.send(
                    TOPIC,
                    code,
                    new ClickMessage(
                            code,
                            Instant.now(),
                            req.getHeader("User-Agent"),
                            req.getHeader("Referer")
                    )
            );

        } catch (Exception e) {
            log.warn("Click not recorded for {}", code, e);
        }
    }
}