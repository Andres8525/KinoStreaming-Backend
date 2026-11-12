package com.kino.backend.repository;

import com.kino.backend.model.Video;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VideoRepository extends JpaRepository<Video, UUID> {
    List<Video> findByCreator_EmailIgnoreCaseOrderByCreatedAtDesc(String email);
    List<Video> findAllByOrderByCreatedAtDesc();
    List<Video> findAllByOrderByViewCountDescCreatedAtDesc();
    List<Video> findAllByOrderByViewCountDescCreatedAtDesc(Pageable pageable);
    List<Video> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title, Pageable pageable);

    @Modifying
    @Query("update Video video set video.viewCount = video.viewCount + 1 where video.id = :videoId")
    int incrementViewCount(@Param("videoId") UUID videoId);
}