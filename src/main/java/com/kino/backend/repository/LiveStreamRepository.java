package com.kino.backend.repository;

import com.kino.backend.model.LiveStream;
import com.kino.backend.model.LiveStreamStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiveStreamRepository extends JpaRepository<LiveStream, UUID> {
    List<LiveStream> findByStatusOrderByStartedAtDesc(LiveStreamStatus status);

    Optional<LiveStream> findFirstByCreator_EmailIgnoreCaseAndStatus(String email, LiveStreamStatus status);

    Optional<LiveStream> findByIdAndCreator_EmailIgnoreCase(UUID id, String email);
}
