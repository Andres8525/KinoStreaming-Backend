package com.kino.backend.grpc;

import com.kino.backend.dto.CategoryInsight;
import com.kino.backend.dto.LiveStreamResponse;
import com.kino.backend.dto.TrendingInsightsResponse;
import com.kino.backend.dto.VideoResponse;
import com.kino.backend.grpc.v1.LiveStream;
import com.kino.backend.grpc.v1.LiveStreamsSnapshot;
import com.kino.backend.grpc.v1.TrendingInsights;
import com.kino.backend.grpc.v1.Video;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

final class GrpcMapper {
    private GrpcMapper() {
    }

    static Video video(VideoResponse video) {
        return Video.newBuilder()
                .setId(video.id().toString())
                .setTitle(text(video.title()))
                .setDescription(text(video.description()))
                .setVideoUrl(text(video.videoUrl()))
                .setThumbnailUrl(text(video.thumbnailUrl()))
                .addAllTags(video.tags())
                .setCategory(video.tags().isEmpty() ? "" : video.tags().get(0))
                .setViewCount(video.viewCount())
                .setCreatorName(video.creator() == null ? "" : text(video.creator().displayName()))
                .setCreatedAt(text(video.createdAt()))
                .setAccessPrice(video.accessPrice() == null ? 0 : video.accessPrice().doubleValue())
                .build();
    }

    static TrendingInsights insights(TrendingInsightsResponse insights) {
        return TrendingInsights.newBuilder()
                .setGeneratedAt(text(insights.generatedAt()))
                .setSource(text(insights.source()))
                .setSummary(text(insights.summary()))
                .addAllMostWatched(insights.mostWatched().stream().map(GrpcMapper::video).toList())
                .addAllRising(insights.rising().stream().map(GrpcMapper::video).toList())
                .addAllTopCategories(insights.topCategories().stream().map(GrpcMapper::category).toList())
                .setTotalViews(insights.totalViews())
                .setLiveNow(insights.liveNow())
                .build();
    }

    static LiveStreamsSnapshot liveStreams(List<LiveStreamResponse> streams, Instant generatedAt) {
        return LiveStreamsSnapshot.newBuilder()
                .setGeneratedAt(generatedAt.toString())
                .addAllStreams(streams.stream().map(GrpcMapper::liveStream).toList())
                .build();
    }

    private static com.kino.backend.grpc.v1.CategoryInsight category(CategoryInsight category) {
        return com.kino.backend.grpc.v1.CategoryInsight.newBuilder()
                .setCategory(text(category.category()))
                .setVideoCount(category.videoCount())
                .setTotalViews(category.totalViews())
                .build();
    }

    private static LiveStream liveStream(LiveStreamResponse stream) {
        return LiveStream.newBuilder()
                .setId(stream.id().toString())
                .setTitle(text(stream.title()))
                .setDescription(text(stream.description()))
                .setCategory(text(stream.category()))
                .setSourceType(text(stream.sourceType()))
                .setPlaybackUrl(text(stream.playbackUrl()))
                .setCreatorName(stream.creator() == null ? "" : text(stream.creator().displayName()))
                .setStartedAt(text(stream.startedAt()))
                .build();
    }

    private static String text(Object value) {
        return Objects.toString(value, "");
    }
}
