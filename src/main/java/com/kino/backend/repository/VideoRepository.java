package com.kino.backend.repository;

import com.kino.backend.model.Video;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, UUID> {
    List<Video> findByCreator_EmailIgnoreCaseOrderByCreatedAtDesc(String email);
    List<Video> findAllByOrderByCreatedAtDesc();
}