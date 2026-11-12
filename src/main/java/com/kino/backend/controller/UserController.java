package com.kino.backend.controller;

import com.kino.backend.dto.UserProfileResponse;
import com.kino.backend.model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal User user) {
        return UserProfileResponse.from(user);
    }
}
