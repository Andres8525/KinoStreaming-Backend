package com.kino.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "interactions")
public class Interaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column
    private Integer rating;

    @Column(nullable = false)
    private Integer watchedSeconds = 0;

    @Column(nullable = false, updatable = false)
    private Instant watchedAt;

    protected Interaction() {
    }

    public Interaction(User user, Video video, Integer rating, Integer watchedSeconds) {
        this.user = user;
        this.video = video;
        this.rating = rating;
        this.watchedSeconds = watchedSeconds;
    }

    @PrePersist
    void onCreate() {
        watchedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Video getVideo() { return video; }
    public Integer getRating() { return rating; }
    public Integer getWatchedSeconds() { return watchedSeconds; }
    public Instant getWatchedAt() { return watchedAt; }
}