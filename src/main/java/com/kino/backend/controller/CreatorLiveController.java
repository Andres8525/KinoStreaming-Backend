package com.kino.backend.controller;

import com.kino.backend.dto.LiveStreamResponse;
import com.kino.backend.dto.StartLiveStreamRequest;
import com.kino.backend.service.LiveStreamService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Creator endpoints to start, check and end their own live broadcast. */
@RestController
@RequestMapping("/api/creator/live")
public class CreatorLiveController {

    private final LiveStreamService liveStreamService;

    public CreatorLiveController(LiveStreamService liveStreamService) {
        this.liveStreamService = liveStreamService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LiveStreamResponse start(@Valid @RequestBody StartLiveStreamRequest request, Principal principal) {
        return liveStreamService.start(principal.getName(), request);
    }

    @GetMapping("/current")
    public ResponseEntity<LiveStreamResponse> current(Principal principal) {
        return liveStreamService.current(principal.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{streamId}/end")
    public LiveStreamResponse end(@PathVariable UUID streamId, Principal principal) {
        return liveStreamService.end(principal.getName(), streamId);
    }
}
