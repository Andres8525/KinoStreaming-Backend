package com.kino.backend.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "videos")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Column(nullable = false, length = 2048)
    private String videoUrl;

    @ElementCollection
    @CollectionTable(name = "video_tags", joinColumns = @JoinColumn(name = "video_id"))
    @Column(name = "tag", nullable = false, length = 40)
    private List<String> tags = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal accessPrice = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal suggestedDonation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Video() {
    }

    public Video(String title, String description, String videoUrl, List<String> tags,
                 BigDecimal accessPrice, BigDecimal suggestedDonation, User creator) {
        this.title = title;
        this.description = description;
        this.videoUrl = videoUrl;
        this.tags = new ArrayList<>(tags);
        this.accessPrice = accessPrice;
        this.suggestedDonation = suggestedDonation;
        this.creator = creator;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getVideoUrl() { return videoUrl; }
    public List<String> getTags() { return List.copyOf(tags); }
    public BigDecimal getAccessPrice() { return accessPrice; }
    public BigDecimal getSuggestedDonation() { return suggestedDonation; }
    public User getCreator() { return creator; }
    public Instant getCreatedAt() { return createdAt; }
}