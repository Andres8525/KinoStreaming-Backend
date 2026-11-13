package com.kino.backend.service;

import com.kino.backend.dto.LiveStreamResponse;
import com.kino.backend.dto.LiveViewerSessionResponse;
import com.kino.backend.dto.PendingViewerResponse;
import com.kino.backend.dto.StartLiveStreamRequest;
import com.kino.backend.model.LiveSourceType;
import com.kino.backend.model.LiveStream;
import com.kino.backend.model.LiveStreamStatus;
import com.kino.backend.model.User;
import com.kino.backend.repository.LiveStreamRepository;
import com.kino.backend.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LiveStreamService {

    private final LiveStreamRepository liveStreamRepository;
    private final UserRepository userRepository;
    private final LiveSignalingService signaling;

    public LiveStreamService(LiveStreamRepository liveStreamRepository, UserRepository userRepository,
                             LiveSignalingService signaling) {
        this.liveStreamRepository = liveStreamRepository;
        this.userRepository = userRepository;
        this.signaling = signaling;
    }

    @Transactional
    public LiveStreamResponse start(String creatorEmail, StartLiveStreamRequest request) {
        User creator = userRepository.findByEmailIgnoreCase(creatorEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el creador"));
        if (liveStreamRepository.findFirstByCreator_EmailIgnoreCaseAndStatus(creatorEmail, LiveStreamStatus.LIVE).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya tienes una transmisión en vivo. Finalízala antes de iniciar otra.");
        }
        LiveSourceType sourceType = request.effectiveSourceType();
        String playbackUrl = sourceType == LiveSourceType.CAMERA ? null : blankToNull(request.playbackUrl());
        if (sourceType == LiveSourceType.HLS && playbackUrl == null) {
            throw new FieldValidationException("playbackUrl", "Indica la URL de reproducción HLS");
        }
        LiveStream stream = liveStreamRepository.save(new LiveStream(creator, request.title().trim(),
                blankToNull(request.description()), request.category().trim(), sourceType, playbackUrl,
                blankToNull(request.thumbnailUrl())));
        if (sourceType == LiveSourceType.CAMERA) {
            signaling.heartbeat(stream.getId());
        }
        return LiveStreamResponse.from(stream);
    }

    @Transactional
    public LiveStreamResponse end(String creatorEmail, UUID streamId) {
        LiveStream stream = liveStreamRepository.findByIdAndCreator_EmailIgnoreCase(streamId, creatorEmail)
                .orElseThrow(LiveStreamService::streamNotFound);
        stream.end();
        signaling.close(streamId);
        return LiveStreamResponse.from(stream);
    }

    @Transactional(readOnly = true)
    public Optional<LiveStreamResponse> current(String creatorEmail) {
        return liveStreamRepository.findFirstByCreator_EmailIgnoreCaseAndStatus(creatorEmail, LiveStreamStatus.LIVE)
                .map(LiveStreamResponse::from);
    }

    /** Live broadcasts; camera broadcasts are listed only while the creator's browser is connected. */
    @Transactional(readOnly = true)
    public List<LiveStreamResponse> listLive() {
        return liveStreamRepository.findByStatusOrderByStartedAtDesc(LiveStreamStatus.LIVE).stream()
                .filter(stream -> stream.getSourceType() != LiveSourceType.CAMERA
                        || signaling.isBroadcasterOnline(stream.getId()))
                .map(LiveStreamResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LiveStreamResponse findById(UUID streamId) {
        return liveStreamRepository.findById(streamId)
                .map(LiveStreamResponse::from)
                .orElseThrow(LiveStreamService::streamNotFound);
    }

    /** Creator side of a camera broadcast: heartbeat plus the viewers waiting for an answer. */
    @Transactional(readOnly = true)
    public List<PendingViewerResponse> pendingViewers(String creatorEmail, UUID streamId) {
        return signaling.pendingViewers(ownLiveCameraStream(creatorEmail, streamId).getId());
    }

    @Transactional(readOnly = true)
    public void answerViewer(String creatorEmail, UUID streamId, UUID sessionId, String answer) {
        signaling.answer(ownLiveCameraStream(creatorEmail, streamId).getId(), sessionId, answer);
    }

    /** Viewer side: hands the viewer's WebRTC offer to the creator's browser. */
    @Transactional(readOnly = true)
    public LiveViewerSessionResponse requestViewing(String viewerEmail, UUID streamId, String offer) {
        LiveStream stream = liveStreamRepository.findById(streamId).orElseThrow(LiveStreamService::streamNotFound);
        if (stream.getStatus() != LiveStreamStatus.LIVE || stream.getSourceType() != LiveSourceType.CAMERA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta transmisión no se emite desde una cámara en vivo");
        }
        String viewerName = userRepository.findByEmailIgnoreCase(viewerEmail)
                .map(User::getDisplayName)
                .orElse("Espectador");
        return signaling.requestViewing(streamId, viewerName, offer);
    }

    public LiveViewerSessionResponse viewerSession(UUID streamId, UUID sessionId) {
        return signaling.viewerSession(streamId, sessionId);
    }

    private LiveStream ownLiveCameraStream(String creatorEmail, UUID streamId) {
        LiveStream stream = liveStreamRepository.findByIdAndCreator_EmailIgnoreCase(streamId, creatorEmail)
                .orElseThrow(LiveStreamService::streamNotFound);
        if (stream.getStatus() != LiveStreamStatus.LIVE || stream.getSourceType() != LiveSourceType.CAMERA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La transmisión no está en vivo desde la cámara");
        }
        return stream;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ResponseStatusException streamNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la transmisión");
    }
}
