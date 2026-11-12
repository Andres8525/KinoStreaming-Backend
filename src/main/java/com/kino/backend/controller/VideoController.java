package com.kino.backend.controller;

import com.kino.backend.dto.VideoResponse;
import com.kino.backend.dto.ViewCountResponse;
import com.kino.backend.model.User;
import com.kino.backend.service.VideoService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping
    public List<VideoResponse> catalog() {
        return videoService.listCatalog();
    }

    @GetMapping("/trending")
    public List<VideoResponse> trending() {
        return videoService.listTrending();
    }

    @GetMapping("/{videoId}")
    public VideoResponse findById(@PathVariable UUID videoId) {
        return videoService.findById(videoId);
    }

    @PostMapping("/{videoId}/views")
    @ResponseStatus(HttpStatus.OK)
    public ViewCountResponse recordView(@PathVariable UUID videoId, @AuthenticationPrincipal User viewer) {
        return new ViewCountResponse(videoService.recordView(videoId, viewer));
    }
}
