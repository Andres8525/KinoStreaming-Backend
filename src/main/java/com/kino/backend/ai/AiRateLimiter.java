package com.kino.backend.ai;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiRateLimiter {
    static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final int requestsPerMinute;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public AiRateLimiter(@Value("${kino.ai.chat-requests-per-minute:12}") int requestsPerMinute) {
        this(requestsPerMinute, Clock.systemUTC());
    }

    AiRateLimiter(int requestsPerMinute, Clock clock) {
        this.requestsPerMinute = Math.max(1, requestsPerMinute);
        this.clock = clock;
    }

    public void acquire(String client) {
        if (!tryAcquire(client)) {
            throw new AiRateLimitException();
        }
    }

    public boolean tryAcquire(String client) {
        Instant now = clock.instant();
        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.values().removeIf(window -> window.expired(now));
        }
        Window window = windows.compute(client == null ? "unknown" : client, (key, current) ->
                current == null || current.expired(now) ? new Window(now, 1) : new Window(current.start(), current.count() + 1));
        return window.count() <= requestsPerMinute;
    }

    public int requestsPerMinute() {
        return requestsPerMinute;
    }

    private record Window(Instant start, int count) {
        boolean expired(Instant now) {
            return !start.plus(WINDOW).isAfter(now);
        }
    }
}
