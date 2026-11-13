package com.kino.backend.controller;

import com.kino.backend.dto.LiveStreamResponse;
import com.kino.backend.service.LiveStreamService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public endpoints to discover and watch live broadcasts. */
@RestController
@RequestMapping("/api/live")
public class LiveStreamController {

    private final LiveStreamService liveStreamService;

    public LiveStreamController(LiveStreamService liveStreamService) {
        this.liveStreamService = liveStreamService;
    }

    @GetMapping
    public List<LiveStreamResponse> live() {
        return liveStreamService.listLive();
    }

    @GetMapping("/{streamId}")
    public LiveStreamResponse findById(@PathVariable UUID streamId) {
        return liveStreamService.findById(streamId);
    }
}
