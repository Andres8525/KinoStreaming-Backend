package com.kino.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AiRateLimiterTest {

    @Test
    void allowsTheConfiguredRequestsPerMinuteForEachClient() {
        AiRateLimiter limiter = new AiRateLimiter(2, Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC));

        assertThat(limiter.tryAcquire("ip:1")).isTrue();
        assertThat(limiter.tryAcquire("ip:1")).isTrue();
        assertThat(limiter.tryAcquire("ip:1")).isFalse();
        assertThat(limiter.tryAcquire("ip:2")).isTrue();
        assertThatThrownBy(() -> limiter.acquire("ip:1")).isInstanceOf(AiRateLimitException.class)
                .hasMessageContaining("Espera un minuto");
    }

    @Test
    void startsANewWindowAfterAMinute() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-15T12:00:00Z"));
        AiRateLimiter limiter = new AiRateLimiter(1, clock);

        assertThat(limiter.tryAcquire("user:ana")).isTrue();
        assertThat(limiter.tryAcquire("user:ana")).isFalse();
        clock.advance(Duration.ofSeconds(61));
        assertThat(limiter.tryAcquire("user:ana")).isTrue();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
