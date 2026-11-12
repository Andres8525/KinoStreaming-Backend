package com.kino.backend.service;

import com.kino.backend.dto.CreateVideoRequest;
import com.kino.backend.dto.CreatorAnalyticsResponse;
import com.kino.backend.dto.VideoResponse;
import com.kino.backend.model.Interaction;
import com.kino.backend.model.User;
import com.kino.backend.model.Video;
import com.kino.backend.repository.InteractionRepository;
import com.kino.backend.repository.UserRepository;
import com.kino.backend.repository.VideoRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final InteractionRepository interactionRepository;

    public VideoService(VideoRepository videoRepository, UserRepository userRepository,
                        InteractionRepository interactionRepository) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.interactionRepository = interactionRepository;
    }

    @Transactional
    public VideoResponse create(String creatorEmail, CreateVideoRequest request) {
        User creator = userRepository.findByEmailIgnoreCase(creatorEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el creador"));
        Video video = new Video(request.title().trim(), request.description().trim(), request.videoUrl().trim(),
                request.thumbnailUrl() == null ? null : request.thumbnailUrl().trim(),
                request.tags().stream().map(String::trim).distinct().toList(), request.accessPrice(),
                request.suggestedDonation(), creator);
        return VideoResponse.from(videoRepository.save(video));
    }

    @Transactional(readOnly = true)
    public List<VideoResponse> listCatalog() {
        return videoRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(VideoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VideoResponse> listTrending() {
        return videoRepository.findAllByOrderByViewCountDescCreatedAtDesc(PageRequest.of(0, 20)).stream()
                .map(VideoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VideoResponse> search(String query) {
        String term = query == null ? "" : query.trim();
        if (term.isEmpty()) {
            return List.of();
        }
        return videoRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(term, PageRequest.of(0, 50))
                .stream()
                .map(VideoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public VideoResponse findById(UUID videoId) {
        return videoRepository.findById(videoId)
                .map(VideoResponse::from)
                .orElseThrow(VideoService::videoNotFound);
    }

    @Transactional
    public long recordView(UUID videoId, User viewer) {
        if (videoRepository.incrementViewCount(videoId) == 0) {
            throw videoNotFound();
        }
        Video video = videoRepository.findById(videoId)
                .orElseThrow(VideoService::videoNotFound);
        if (!interactionRepository.existsByUser_IdAndVideo_Id(viewer.getId(), videoId)) {
            interactionRepository.save(new Interaction(viewer, video, null, 0));
        }
        return video.getViewCount();
    }

    @Transactional
    public void delete(String creatorEmail, UUID videoId) {
        Video video = videoRepository.findByIdAndCreator_EmailIgnoreCase(videoId, creatorEmail)
                .orElseThrow(VideoService::videoNotFound);
        interactionRepository.deleteByVideo_Id(video.getId());
        videoRepository.delete(video);
    }

    @Transactional(readOnly = true)
    public List<VideoResponse> listByCreator(String creatorEmail) {
        return videoRepository.findByCreator_EmailIgnoreCaseOrderByCreatedAtDesc(creatorEmail).stream()
                .map(VideoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CreatorAnalyticsResponse analytics(String creatorEmail) {
        List<VideoResponse> videos = videoRepository.findByCreator_EmailIgnoreCaseOrderByCreatedAtDesc(creatorEmail)
                .stream()
                .map(VideoResponse::from)
                .toList();
        long totalViews = videos.stream().mapToLong(VideoResponse::viewCount).sum();
        List<VideoResponse> topVideos = videos.stream()
                .sorted(Comparator.comparingLong(VideoResponse::viewCount).reversed())
                .limit(5)
                .toList();
        return new CreatorAnalyticsResponse(videos.size(), totalViews, topVideos);
    }

    private static ResponseStatusException videoNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el video");
    }
}
