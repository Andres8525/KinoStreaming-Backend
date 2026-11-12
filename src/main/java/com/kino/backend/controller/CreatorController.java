package com.kino.backend.controller;

import com.kino.backend.dto.CreateVideoRequest;
import com.kino.backend.dto.CreatorAnalyticsResponse;
import com.kino.backend.dto.VideoResponse;
import com.kino.backend.service.VideoService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/creator/videos")
public class CreatorController {

    private final VideoService videoService;

    public CreatorController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VideoResponse create(@Valid @RequestBody CreateVideoRequest request, Principal principal) {
        return videoService.create(principal.getName(), request);
    }

    @GetMapping
    public List<VideoResponse> listMine(Principal principal) {
        return videoService.listByCreator(principal.getName());
    }

    @DeleteMapping("/{videoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID videoId, Principal principal) {
        videoService.delete(principal.getName(), videoId);
    }

    @GetMapping("/analytics")
    public CreatorAnalyticsResponse analytics(Principal principal) {
        return videoService.analytics(principal.getName());
    }
}