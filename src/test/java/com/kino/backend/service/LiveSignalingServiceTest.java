package com.kino.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class LiveSignalingServiceTest {

    private final MutableClock clock = new MutableClock();
    private final LiveSignalingService signaling = new LiveSignalingService(clock);
    private final UUID streamId = UUID.randomUUID();

    @Test
    void relaysTheOfferToTheCreatorAndTheAnswerBackToTheViewer() {
        signaling.heartbeat(streamId);
        UUID sessionId = signaling.requestViewing(streamId, "Edison", "offer-sdp").sessionId();

        var pending = signaling.pendingViewers(streamId);
        assertEquals(1, pending.size());
        assertEquals("Edison", pending.get(0).viewerName());
        assertEquals("offer-sdp", pending.get(0).offer());
        assertNull(signaling.viewerSession(streamId, sessionId).answer());

        signaling.answer(streamId, sessionId, "answer-sdp");

        assertEquals("answer-sdp", signaling.viewerSession(streamId, sessionId).answer());
        assertTrue(signaling.pendingViewers(streamId).isEmpty());
        assertEquals(1, signaling.viewerCount(streamId));
    }

    @Test
    void theBroadcastIsOfflineWhenTheCreatorStopsPolling() {
        assertFalse(signaling.isBroadcasterOnline(streamId));
        signaling.heartbeat(streamId);
        assertTrue(signaling.isBroadcasterOnline(streamId));

        clock.advance(LiveSignalingService.BROADCASTER_TIMEOUT.plusSeconds(1));

        assertFalse(signaling.isBroadcasterOnline(streamId));
        var error = assertThrows(ResponseStatusException.class,
                () -> signaling.requestViewing(streamId, "Edison", "offer-sdp"));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void unansweredViewersExpire() {
        signaling.heartbeat(streamId);
        UUID sessionId = signaling.requestViewing(streamId, "Edison", "offer-sdp").sessionId();

        clock.advance(LiveSignalingService.UNANSWERED_TIMEOUT.plusSeconds(1));

        assertTrue(signaling.pendingViewers(streamId).isEmpty());
        var error = assertThrows(ResponseStatusException.class, () -> signaling.viewerSession(streamId, sessionId));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void closingTheBroadcastForgetsItsViewers() {
        signaling.heartbeat(streamId);
        UUID sessionId = signaling.requestViewing(streamId, "Edison", "offer-sdp").sessionId();

        signaling.close(streamId);

        assertFalse(signaling.isBroadcasterOnline(streamId));
        assertThrows(ResponseStatusException.class, () -> signaling.answer(streamId, sessionId, "answer-sdp"));
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-13T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
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
            return now;
        }
    }
}
