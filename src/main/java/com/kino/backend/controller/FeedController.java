package com.kino.backend.controller;

import com.kino.backend.dto.FeedResponse;
import com.kino.backend.model.User;
import com.kino.backend.service.FeedService;
import java.security.Principal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping("/recommendations")
    public FeedResponse recommendations(@AuthenticationPrincipal User user, Principal principal) {
        return feedService.recommendations(user.getId(), principal.getName());
    }
}