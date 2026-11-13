package com.kino.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "live_streams", indexes = {
        @Index(name = "idx_live_streams_status_started_at", columnList = "status, started_at"),
        @Index(name = "idx_live_streams_creator_id", columnList = "creator_id")
})
public class LiveStream {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 60)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LiveSourceType sourceType = LiveSourceType.HLS;

    @Column(length = 2048)
    private String playbackUrl;

    @Column(length = 2048)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LiveStreamStatus status = LiveStreamStatus.LIVE;

    @Column(nullable = false, updatable = false)
    private Instant startedAt;

    @Column
    private Instant endedAt;

    protected LiveStream() {
    }

    public LiveStream(User creator, String title, String description, String category,
                      String playbackUrl, String thumbnailUrl) {
        this(creator, title, description, category, LiveSourceType.HLS, playbackUrl, thumbnailUrl);
    }

    public LiveStream(User creator, String title, String description, String category, LiveSourceType sourceType,
                      String playbackUrl, String thumbnailUrl) {
        this.creator = creator;
        this.sourceType = sourceType;
        this.title = title;
        this.description = description;
        this.category = category;
        this.playbackUrl = playbackUrl;
        this.thumbnailUrl = thumbnailUrl;
    }

    @PrePersist
    void onCreate() {
        startedAt = Instant.now();
    }

    public void end() {
        if (status == LiveStreamStatus.LIVE) {
            status = LiveStreamStatus.ENDED;
            endedAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public User getCreator() { return creator; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public LiveSourceType getSourceType() { return sourceType; }
    public String getPlaybackUrl() { return playbackUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public LiveStreamStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
}
