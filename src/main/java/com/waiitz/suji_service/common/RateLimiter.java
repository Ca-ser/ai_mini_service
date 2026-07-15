package com.waiitz.suji_service.common;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class RateLimiter {

    private final Map<String, Deque<Instant>> requestLogs = new ConcurrentHashMap<>();

    private final int maxRequests = 10;
    private final Duration window = Duration.ofMinutes(1);

    public boolean tryAcquire(String key) {
        Instant now = Instant.now();
        Deque<Instant> log = requestLogs.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        synchronized (log) {
            while (!log.isEmpty() && log.peekFirst().isBefore(now.minus(window))) {
                log.pollFirst();
            }
            if (log.size() >= maxRequests) {
                return false;
            }
            log.addLast(now);
            return true;
        }
    }
}
