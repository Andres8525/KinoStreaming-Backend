package com.kino.backend.service;

import com.kino.backend.dto.FeedResponse;
import com.kino.backend.dto.VideoResponse;
import com.kino.backend.model.Interaction;
import com.kino.backend.model.Video;
import com.kino.backend.repository.InteractionRepository;
import com.kino.backend.repository.VideoRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedService {

    static final String STRATEGY = "hybrid-collaborative-popularity";
    private static final int FEED_SIZE = 20;
    private static final long MAX_POPULARITY_SCORE = 1_000_000L;
    private static final long MAX_COLLABORATIVE_SCORE = 100_000L;
    private static final long COLLABORATIVE_WEIGHT = 10L;
    private static final long MAX_WATCH_MINUTES_SCORE = 10L;

    private final VideoRepository videoRepository;
    private final InteractionRepository interactionRepository;

    public FeedService(VideoRepository videoRepository, InteractionRepository interactionRepository) {
        this.videoRepository = videoRepository;
        this.interactionRepository = interactionRepository;
    }

    @Transactional(readOnly = true)
    public FeedResponse recommendations(UUID userId, String email) {
        Set<UUID> watchedVideoIds = interactionRepository.findByUser_Id(userId).stream()
                .map(interaction -> interaction.getVideo().getId())
                .collect(Collectors.toSet());

        Set<UUID> similarViewerIds = watchedVideoIds.isEmpty() ? Set.of()
                : interactionRepository.findByVideo_IdIn(watchedVideoIds).stream()
                        .map(interaction -> interaction.getUser().getId())
                        .filter(otherUserId -> !otherUserId.equals(userId))
                        .collect(Collectors.toSet());

        Map<UUID, Long> collaborativeScores = new HashMap<>();
        List<Interaction> similarInteractions = similarViewerIds.isEmpty() ? List.of()
                : interactionRepository.findByUser_IdIn(similarViewerIds);
        similarInteractions.stream()
                .filter(interaction -> !watchedVideoIds.contains(interaction.getVideo().getId()))
                .forEach(interaction -> collaborativeScores.merge(
                        interaction.getVideo().getId(), score(interaction), Long::sum));

        List<Video> rankedVideos = videoRepository.findAllByOrderByViewCountDescCreatedAtDesc().stream()
                .filter(video -> !video.getCreator().getEmail().equalsIgnoreCase(email))
                .filter(video -> !watchedVideoIds.contains(video.getId()))
                .sorted(Comparator
                        .comparingLong((Video video) -> rank(video, collaborativeScores)).reversed()
                        .thenComparing(Video::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(FEED_SIZE)
                .toList();

        return new FeedResponse(STRATEGY, Instant.now(),
                rankedVideos.stream().map(VideoResponse::from).toList());
    }

    private long rank(Video video, Map<UUID, Long> collaborativeScores) {
        long popularityScore = Math.min(video.getViewCount(), MAX_POPULARITY_SCORE);
        long collaborativeScore = Math.min(collaborativeScores.getOrDefault(video.getId(), 0L), MAX_COLLABORATIVE_SCORE);
        return popularityScore + collaborativeScore * COLLABORATIVE_WEIGHT;
    }

    private long score(Interaction interaction) {
        long ratingScore = interaction.getRating() == null ? 1 : Math.max(1, interaction.getRating());
        return ratingScore + Math.min(interaction.getWatchedSeconds() / 60L, MAX_WATCH_MINUTES_SCORE);
    }
}