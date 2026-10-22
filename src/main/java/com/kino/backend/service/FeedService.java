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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedService {

    private final VideoRepository videoRepository;
    private final InteractionRepository interactionRepository;

    public FeedService(VideoRepository videoRepository, InteractionRepository interactionRepository) {
        this.videoRepository = videoRepository;
        this.interactionRepository = interactionRepository;
    }

    @Transactional(readOnly = true)
    public FeedResponse recommendations(UUID userId, String email) {
        List<Interaction> interactions = interactionRepository.findAll();
        Set<UUID> watchedVideoIds = interactions.stream()
                .filter(interaction -> interaction.getUser().getId().equals(userId))
                .map(interaction -> interaction.getVideo().getId())
                .collect(java.util.stream.Collectors.toSet());

        Set<UUID> similarViewerIds = new HashSet<>();
        interactions.stream()
                .filter(interaction -> watchedVideoIds.contains(interaction.getVideo().getId()))
                .map(interaction -> interaction.getUser().getId())
                .filter(otherUserId -> !otherUserId.equals(userId))
                .forEach(similarViewerIds::add);

        Map<UUID, Long> collaborativeScores = new HashMap<>();
        interactions.stream()
                .filter(interaction -> similarViewerIds.contains(interaction.getUser().getId()))
                .filter(interaction -> !watchedVideoIds.contains(interaction.getVideo().getId()))
                .forEach(interaction -> collaborativeScores.merge(
                        interaction.getVideo().getId(), score(interaction), Long::sum));

        List<Video> rankedVideos = videoRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(video -> !video.getCreator().getEmail().equalsIgnoreCase(email))
                .filter(video -> !watchedVideoIds.contains(video.getId()))
                .sorted(Comparator
                        .comparingLong((Video video) -> collaborativeScores.getOrDefault(video.getId(), 0L)).reversed()
                        .thenComparing(Video::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(20)
                .toList();

        return new FeedResponse("collaborative-filtering-demo", Instant.now(),
                rankedVideos.stream().map(VideoResponse::from).toList());
    }

    private long score(Interaction interaction) {
        long ratingScore = interaction.getRating() == null ? 1 : Math.max(1, interaction.getRating());
        return ratingScore + Math.min(interaction.getWatchedSeconds() / 60L, 10L);
    }
}