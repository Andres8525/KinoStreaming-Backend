package com.kino.backend.repository;

import com.kino.backend.model.Interaction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteractionRepository extends JpaRepository<Interaction, UUID> {
    boolean existsByUser_IdAndVideo_Id(UUID userId, UUID videoId);
}