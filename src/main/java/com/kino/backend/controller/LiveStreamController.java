package com.kino.backend.controller;

import com.kino.backend.dto.LiveStreamResponse;
import com.kino.backend.dto.LiveViewerSessionResponse;
import com.kino.backend.dto.SessionDescriptionRequest;
import com.kino.backend.service.LiveStreamService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints to discover and watch live broadcasts. Listing is public; connecting to a camera broadcast
 * ({@code /viewers}) requires a signed-in account.
 */
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

    /** Sends the viewer's WebRTC offer to the creator's browser; poll the returned session for the answer. */
    @PostMapping("/{streamId}/viewers")
    @ResponseStatus(HttpStatus.CREATED)
    public LiveViewerSessionResponse requestViewing(@PathVariable UUID streamId,
                                                    @Valid @RequestBody SessionDescriptionRequest request,
                                                    Principal principal) {
        return liveStreamService.requestViewing(principal.getName(), streamId, request.sdp());
    }

    @GetMapping("/{streamId}/viewers/{sessionId}")
    public LiveViewerSessionResponse viewerSession(@PathVariable UUID streamId, @PathVariable UUID sessionId) {
        return liveStreamService.viewerSession(streamId, sessionId);
    }
}
