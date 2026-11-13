package com.kino.backend.service;

import com.kino.backend.dto.LiveViewerSessionResponse;
import com.kino.backend.dto.PendingViewerResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Relays the WebRTC connection setup between a creator broadcasting from the browser camera and each viewer.
 *
 * <p>The video never passes through Kino: the viewer's browser sends an SDP offer, the creator's browser
 * (which polls for pending viewers) answers it, and from then on both browsers talk directly. ICE candidates
 * are embedded in the SDP (non-trickle ICE), so one offer and one answer are enough.
 *
 * <p>The state is kept in memory, which is enough for the single backend instance Kino runs on. The creator's
 * polling doubles as a heartbeat: a camera broadcast whose creator has not polled for {@link #BROADCASTER_TIMEOUT}
 * (tab closed, connection lost) is considered offline and hidden from the live list.
 */
@Service
public class LiveSignalingService {

    static final Duration BROADCASTER_TIMEOUT = Duration.ofSeconds(30);
    static final Duration UNANSWERED_TIMEOUT = Duration.ofSeconds(60);
    static final Duration ANSWERED_RETENTION = Duration.ofMinutes(2);
    static final int MAX_PENDING_PER_STREAM = 50;

    private final Clock clock;
    private final Map<UUID, Broadcast> broadcasts = new ConcurrentHashMap<>();

    @Autowired
    public LiveSignalingService() {
        this(Clock.systemUTC());
    }

    LiveSignalingService(Clock clock) {
        this.clock = clock;
    }

    /** Records that the creator's browser is connected (called when it starts and on every poll). */
    public void heartbeat(UUID streamId) {
        broadcasts.computeIfAbsent(streamId, id -> new Broadcast()).lastSeen = clock.instant();
    }

    /** Records that the creator's browser is connected and returns the viewers waiting for an answer. */
    public List<PendingViewerResponse> pendingViewers(UUID streamId) {
        heartbeat(streamId);
        Broadcast broadcast = broadcasts.get(streamId);
        Instant now = clock.instant();
        broadcast.removeExpired(now);
        return broadcast.sessions.values().stream()
                .filter(session -> session.answer == null)
                .sorted(Comparator.comparing(session -> session.createdAt))
                .map(session -> new PendingViewerResponse(session.id, session.viewerName, session.offer))
                .toList();
    }

    public boolean isBroadcasterOnline(UUID streamId) {
        Broadcast broadcast = broadcasts.get(streamId);
        return broadcast != null && broadcast.lastSeen != null
                && broadcast.lastSeen.isAfter(clock.instant().minus(BROADCASTER_TIMEOUT));
    }

    /** Number of viewers whose connection was answered recently (an approximation of the current audience). */
    public int viewerCount(UUID streamId) {
        Broadcast broadcast = broadcasts.get(streamId);
        if (broadcast == null) {
            return 0;
        }
        return (int) broadcast.sessions.values().stream().filter(session -> session.answer != null).count();
    }

    public LiveViewerSessionResponse requestViewing(UUID streamId, String viewerName, String offer) {
        if (!isBroadcasterOnline(streamId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El creador no está transmitiendo desde su cámara en este momento. Inténtalo de nuevo en unos segundos.");
        }
        Broadcast broadcast = broadcasts.get(streamId);
        Instant now = clock.instant();
        broadcast.removeExpired(now);
        long pending = broadcast.sessions.values().stream().filter(session -> session.answer == null).count();
        if (pending >= MAX_PENDING_PER_STREAM) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Hay demasiados espectadores conectándose a la vez. Inténtalo de nuevo en unos segundos.");
        }
        ViewerSession session = new ViewerSession(UUID.randomUUID(), viewerName, offer, now);
        broadcast.sessions.put(session.id, session);
        return new LiveViewerSessionResponse(session.id, null);
    }

    public LiveViewerSessionResponse viewerSession(UUID streamId, UUID sessionId) {
        ViewerSession session = findSession(streamId, sessionId).orElseThrow(LiveSignalingService::sessionNotFound);
        return new LiveViewerSessionResponse(session.id, session.answer);
    }

    public void answer(UUID streamId, UUID sessionId, String answer) {
        ViewerSession session = findSession(streamId, sessionId).orElseThrow(LiveSignalingService::sessionNotFound);
        session.answer = answer;
        session.answeredAt = clock.instant();
    }

    /** Forgets everything about a broadcast once it ends. */
    public void close(UUID streamId) {
        broadcasts.remove(streamId);
    }

    private Optional<ViewerSession> findSession(UUID streamId, UUID sessionId) {
        Broadcast broadcast = broadcasts.get(streamId);
        return broadcast == null ? Optional.empty() : Optional.ofNullable(broadcast.sessions.get(sessionId));
    }

    private static ResponseStatusException sessionNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,
                "La conexión con la transmisión caducó. Vuelve a intentarlo.");
    }

    private static final class Broadcast {
        private final Map<UUID, ViewerSession> sessions = new ConcurrentHashMap<>();
        private volatile Instant lastSeen;

        private void removeExpired(Instant now) {
            sessions.values().removeIf(session -> session.answer == null
                    ? session.createdAt.isBefore(now.minus(UNANSWERED_TIMEOUT))
                    : session.answeredAt.isBefore(now.minus(ANSWERED_RETENTION)));
        }
    }

    private static final class ViewerSession {
        private final UUID id;
        private final String viewerName;
        private final String offer;
        private final Instant createdAt;
        private volatile String answer;
        private volatile Instant answeredAt;

        private ViewerSession(UUID id, String viewerName, String offer, Instant createdAt) {
            this.id = id;
            this.viewerName = viewerName;
            this.offer = offer;
            this.createdAt = createdAt;
        }
    }
}
