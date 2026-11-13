package com.kino.backend.service;

import com.kino.backend.dto.LiveStreamResponse;
import com.kino.backend.dto.StartLiveStreamRequest;
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

    public LiveStreamService(LiveStreamRepository liveStreamRepository, UserRepository userRepository) {
        this.liveStreamRepository = liveStreamRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public LiveStreamResponse start(String creatorEmail, StartLiveStreamRequest request) {
        User creator = userRepository.findByEmailIgnoreCase(creatorEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el creador"));
        if (liveStreamRepository.findFirstByCreator_EmailIgnoreCaseAndStatus(creatorEmail, LiveStreamStatus.LIVE).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya tienes una transmisión en vivo. Finalízala antes de iniciar otra.");
        }
        LiveStream stream = new LiveStream(creator, request.title().trim(), blankToNull(request.description()),
                request.category().trim(), request.playbackUrl().trim(), blankToNull(request.thumbnailUrl()));
        return LiveStreamResponse.from(liveStreamRepository.save(stream));
    }

    @Transactional
    public LiveStreamResponse end(String creatorEmail, UUID streamId) {
        LiveStream stream = liveStreamRepository.findByIdAndCreator_EmailIgnoreCase(streamId, creatorEmail)
                .orElseThrow(LiveStreamService::streamNotFound);
        stream.end();
        return LiveStreamResponse.from(stream);
    }

    @Transactional(readOnly = true)
    public Optional<LiveStreamResponse> current(String creatorEmail) {
        return liveStreamRepository.findFirstByCreator_EmailIgnoreCaseAndStatus(creatorEmail, LiveStreamStatus.LIVE)
                .map(LiveStreamResponse::from);
    }

    @Transactional(readOnly = true)
    public List<LiveStreamResponse> listLive() {
        return liveStreamRepository.findByStatusOrderByStartedAtDesc(LiveStreamStatus.LIVE).stream()
                .map(LiveStreamResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LiveStreamResponse findById(UUID streamId) {
        return liveStreamRepository.findById(streamId)
                .map(LiveStreamResponse::from)
                .orElseThrow(LiveStreamService::streamNotFound);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ResponseStatusException streamNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la transmisión");
    }
}
