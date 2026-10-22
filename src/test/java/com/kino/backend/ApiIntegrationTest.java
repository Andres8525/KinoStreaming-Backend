package com.kino.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kino.backend.dto.AuthResponse;
import com.kino.backend.dto.CreateVideoRequest;
import com.kino.backend.dto.CreateInteractionRequest;
import com.kino.backend.dto.LoginRequest;
import com.kino.backend.dto.RegisterRequest;
import com.kino.backend.model.UserRole;
import com.kino.backend.repository.UserRepository;
import com.kino.backend.security.JwtService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private Validator validator;

    @Test
    void registerIssuesViewerTokenAndRecommendationsAcceptIt() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new RegisterRequest("Kino Viewer", "viewer@example.com", "secure-password-1", UserRole.CREATOR));

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("VIEWER"))
                .andReturn().getResponse().getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(response, AuthResponse.class);
        mockMvc.perform(get("/api/feed/recommendations")
                        .header("Authorization", "Bearer " + authResponse.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("collaborative-filtering-demo"))
                .andExpect(jsonPath("$.recommendations").isArray());
    }

    @Test
    void corsPreflightAllowsAngularAuthorizationHeader() throws Exception {
        mockMvc.perform(options("/api/feed/recommendations")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Headers",
                        org.hamcrest.Matchers.containsStringIgnoringCase("authorization")));
    }

        @Test
        void loginWithIncorrectPasswordReturnsUnauthorizedJson() throws Exception {
                mockMvc.perform(post("/api/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(
                                                                new LoginRequest("missing@example.com", "wrong-password"))))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Correo o contraseña incorrectos"));
        }

    @Test
        void creatorEndpointRejectsUnauthenticatedRequest() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "title", "Video test",
                "description", "Descripción",
                                "videoUrl", "https://cdn.example.com/video.mp4",
                                "tags", List.of("test"),
                                "accessPrice", "4.99"));

        mockMvc.perform(post("/api/creator/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidVideoUrlReturnsFieldValidationErrorForCreator() throws Exception {
        var creator = userRepository.save(new com.kino.backend.model.User(
                "Kino Creator", "creator-validation@example.com",
                passwordEncoder.encode("secure-password-1"), UserRole.CREATOR));
        String token = jwtService.generateToken(creator);
        String payload = objectMapper.writeValueAsString(Map.of(
                "title", "Video test",
                "description", "Descripción",
                "videoUrl", "not-a-url",
                "tags", List.of("test"),
                "accessPrice", "4.99"));

        mockMvc.perform(post("/api/creator/videos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.videoUrl").exists());
    }

    @Test
    void creatorCanPublishVideoWithMonetization() throws Exception {
        var creator = userRepository.save(new com.kino.backend.model.User(
                "Publishing Creator", "creator-publish@example.com",
                passwordEncoder.encode("secure-password-1"), UserRole.CREATOR));
        String token = jwtService.generateToken(creator);
        String payload = objectMapper.writeValueAsString(new CreateVideoRequest(
                "A Kino Short", "Independent short film", "https://cdn.example.com/short.mp4",
                List.of("Cortometraje", "Indie"), new java.math.BigDecimal("4.99"),
                new java.math.BigDecimal("2.00")));

        mockMvc.perform(post("/api/creator/videos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("A Kino Short"))
                                .andExpect(jsonPath("$.creator.displayName").value("Publishing Creator"))
                .andExpect(jsonPath("$.accessPrice").value(4.99))
                .andExpect(jsonPath("$.suggestedDonation").value(2.00));
    }

        @Test
        void interactionDtoValidatesRatingAndWatchDuration() {
                var invalidInteraction = new CreateInteractionRequest(UUID.randomUUID(), 6, -1);

                var violations = validator.validate(invalidInteraction);

                org.junit.jupiter.api.Assertions.assertEquals(2, violations.size());
        }
}