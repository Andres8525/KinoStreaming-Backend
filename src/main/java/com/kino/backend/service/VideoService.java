package com.kino.backend.service;

import com.kino.backend.dto.CreateVideoRequest;
import com.kino.backend.dto.VideoResponse;
import com.kino.backend.model.User;
import com.kino.backend.model.Video;
import com.kino.backend.repository.UserRepository;
import com.kino.backend.repository.VideoRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    public VideoService(VideoRepository videoRepository, UserRepository userRepository) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public VideoResponse create(String creatorEmail, CreateVideoRequest request) {
        User creator = userRepository.findByEmailIgnoreCase(creatorEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el creador"));
        Video video = new Video(request.title().trim(), request.description().trim(), request.videoUrl().trim(),
                request.tags().stream().map(String::trim).distinct().toList(), request.accessPrice(),
                request.suggestedDonation(), creator);
        return VideoResponse.from(videoRepository.save(video));
    }

    @Transactional(readOnly = true)
    public List<VideoResponse> listByCreator(String creatorEmail) {
        return videoRepository.findByCreator_EmailIgnoreCaseOrderByCreatedAtDesc(creatorEmail).stream()
                .map(VideoResponse::from)
                .toList();
    }
}