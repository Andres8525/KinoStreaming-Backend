package com.kino.backend.repository;

import com.kino.backend.model.Interaction;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteractionRepository extends JpaRepository<Interaction, UUID> {
    boolean existsByUser_IdAndVideo_Id(UUID userId, UUID videoId);

    List<Interaction> findByUser_Id(UUID userId);

    List<Interaction> findByVideo_IdIn(Collection<UUID> videoIds);

    List<Interaction> findByUser_IdIn(Collection<UUID> userIds);

    void deleteByVideo_Id(UUID videoId);
}
