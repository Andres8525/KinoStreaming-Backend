package com.kino.backend.grpc;

import com.kino.backend.dto.VideoResponse;
import com.kino.backend.grpc.v1.CatalogServiceGrpc;
import com.kino.backend.grpc.v1.GetVideoRequest;
import com.kino.backend.grpc.v1.ListTrendingVideosRequest;
import com.kino.backend.grpc.v1.ListTrendingVideosResponse;
import com.kino.backend.grpc.v1.Video;
import com.kino.backend.service.VideoService;
import io.grpc.stub.StreamObserver;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CatalogGrpcService extends CatalogServiceGrpc.CatalogServiceImplBase {
    static final int DEFAULT_LIMIT = 10;
    static final int MAX_LIMIT = 50;

    private final VideoService videoService;

    public CatalogGrpcService(VideoService videoService) {
        this.videoService = videoService;
    }

    @Override
    public void listTrendingVideos(ListTrendingVideosRequest request,
                                   StreamObserver<ListTrendingVideosResponse> responseObserver) {
        try {
            if (request.getLimit() < 0 || request.getLimit() > MAX_LIMIT) {
                throw GrpcErrors.invalidArgument("limit debe estar entre 0 y " + MAX_LIMIT);
            }
            int limit = request.getLimit() == 0 ? DEFAULT_LIMIT : request.getLimit();
            String category = request.getCategory().trim().toLowerCase(Locale.ROOT);
            ListTrendingVideosResponse response = ListTrendingVideosResponse.newBuilder()
                    .addAllVideos(videoService.listTrending().stream()
                            .filter(video -> category.isEmpty() || hasTag(video, category))
                            .limit(limit)
                            .map(GrpcMapper::video)
                            .toList())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (RuntimeException exception) {
            responseObserver.onError(GrpcErrors.toStatus(exception));
        }
    }

    @Override
    public void getVideo(GetVideoRequest request, StreamObserver<Video> responseObserver) {
        try {
            responseObserver.onNext(GrpcMapper.video(videoService.findById(parseId(request.getId()))));
            responseObserver.onCompleted();
        } catch (RuntimeException exception) {
            responseObserver.onError(GrpcErrors.toStatus(exception));
        }
    }

    private static boolean hasTag(VideoResponse video, String category) {
        return video.tags().stream().anyMatch(tag -> tag.toLowerCase(Locale.ROOT).equals(category));
    }

    private static UUID parseId(String id) {
        try {
            return UUID.fromString(id.trim());
        } catch (IllegalArgumentException exception) {
            throw GrpcErrors.invalidArgument("id debe ser un UUID válido");
        }
    }
}
