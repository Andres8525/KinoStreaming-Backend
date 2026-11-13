package com.kino.backend.dto;

import com.kino.backend.model.LiveSourceType;
import com.kino.backend.model.LiveStream;
import com.kino.backend.model.LiveStreamStatus;
import java.time.Instant;
import java.util.UUID;

public record LiveStreamResponse(
        UUID id,
        String title,
        String description,
        String category,
        LiveSourceType sourceType,
        String playbackUrl,
        String thumbnailUrl,
        LiveStreamStatus status,
        CreatorSummaryResponse creator,
        Instant startedAt,
        Instant endedAt) {

    public static LiveStreamResponse from(LiveStream stream) {
        return new LiveStreamResponse(stream.getId(), stream.getTitle(), stream.getDescription(), stream.getCategory(),
                stream.getSourceType(), stream.getPlaybackUrl(), stream.getThumbnailUrl(), stream.getStatus(),
                CreatorSummaryResponse.from(stream.getCreator()), stream.getStartedAt(), stream.getEndedAt());
    }
}
