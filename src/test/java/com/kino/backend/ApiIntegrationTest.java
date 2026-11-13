package com.kino.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
                .andExpect(jsonPath("$.strategy").value("hybrid-collaborative-popularity"))
                .andExpect(jsonPath("$.recommendations").isArray());
    }

    @Test
    void registrationRejectsAnEmailThatIsAlreadyRegistered() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new RegisterRequest("Duplicate Viewer", "duplicate@example.com", "secure-password-1", null));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El correo ya está registrado"));
    }

    @Test
    void registrationNormalizesProfileAndStoresBcryptPassword() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new RegisterRequest("  Kino Member  ", "MEMBER@example.com", "secure-password-1", null));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("member@example.com"));

        var savedUser = userRepository.findByEmailIgnoreCase("member@example.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("Kino Member", savedUser.getDisplayName());
        org.junit.jupiter.api.Assertions.assertNotEquals("secure-password-1", savedUser.getPassword());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(
                "secure-password-1", savedUser.getPassword()));
    }

    @Test
    void loginReturnsBearerTokenAndBasicProfile() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "Login Viewer", "login-viewer@example.com", "secure-password-1", null))))
                .andExpect(status().isCreated());

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("login-viewer@example.com", "secure-password-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("login-viewer@example.com"))
                .andExpect(jsonPath("$.role").value("VIEWER"))
                .andReturn().getResponse().getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(response, AuthResponse.class);
        mockMvc.perform(get("/api/feed/recommendations")
                        .header("Authorization", "Bearer " + authResponse.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void loginAcceptsEmailInDifferentLetterCase() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "Case Viewer", "case-viewer@example.com", "secure-password-1", null))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("CASE-VIEWER@EXAMPLE.COM", "secure-password-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("case-viewer@example.com"));
    }

    @Test
    void registrationRejectsPasswordsBelowMinimumLength() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "displayName", "Invalid Viewer",
                "email", "invalid-password@example.com",
                "password", "short"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    void malformedJsonBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cuerpo de la solicitud no es un JSON válido"));
    }

    @Test
    void invalidVideoIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/videos/{videoId}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.videoId").exists());
    }

    @Test
    void unknownVideoIdReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/videos/{videoId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró el video"));
    }

    @Test
    void healthEndpointIsPublicAndReportsDatabase() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"));
    }

    @Test
    void unknownRouteReturnsJsonNotFound() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La ruta solicitada no existe"));
    }

    @Test
    void unsupportedMethodReturnsJsonMethodNotAllowed() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void viewerTokenCannotAccessCreatorEndpoints() throws Exception {
        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "Viewer Role", "viewer-role@example.com", "secure-password-1", UserRole.CREATOR))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(registerResponse, AuthResponse.class);

        mockMvc.perform(get("/api/creator/videos")
                        .header("Authorization", "Bearer " + authResponse.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("No tienes permisos para realizar esta acción"));
    }

    @Test
    void unauthenticatedRequestReturnsJsonError() throws Exception {
        mockMvc.perform(get("/api/feed/recommendations"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type",
                        org.hamcrest.Matchers.startsWith(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Autenticación requerida"));
    }

    @Test
    void protectedFeedRejectsMalformedJwt() throws Exception {
        mockMvc.perform(get("/api/feed/recommendations")
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedFeedRejectsTokenForDeletedAccount() throws Exception {
        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "Removed Viewer", "removed-viewer@example.com", "secure-password-1", null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(registerResponse, AuthResponse.class);
        userRepository.delete(userRepository.findByEmailIgnoreCase("removed-viewer@example.com").orElseThrow());

        mockMvc.perform(get("/api/feed/recommendations")
                        .header("Authorization", "Bearer " + authResponse.accessToken()))
                .andExpect(status().isUnauthorized());
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
    void corsResponseExposesAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/feed/recommendations")
                        .header("Origin", "http://localhost:4200"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Expose-Headers", "Authorization"));
    }

    @Test
    void corsPreflightAllowsAngularRegistrationPost() throws Exception {
        mockMvc.perform(options("/api/auth/register")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Methods",
                        org.hamcrest.Matchers.containsStringIgnoringCase("POST")));
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
                "https://cdn.example.com/short.jpg",
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
                .andExpect(jsonPath("$.thumbnailUrl").value("https://cdn.example.com/short.jpg"))
                .andExpect(jsonPath("$.viewCount").value(0))
                .andExpect(jsonPath("$.suggestedDonation").value(2.00));
    }

    @Test
    void catalogPlaybackViewsAndCreatorAnalyticsUsePersistedVideoData() throws Exception {
        var creator = userRepository.save(new com.kino.backend.model.User(
                "Streaming Creator", "creator-streaming@example.com",
                passwordEncoder.encode("secure-password-1"), UserRole.CREATOR));
        String token = jwtService.generateToken(creator);
        String payload = objectMapper.writeValueAsString(new CreateVideoRequest(
                "Streaming Short", "A film that can be streamed", "https://cdn.example.com/stream.mp4",
                "https://cdn.example.com/stream.jpg", List.of("Documental"),
                java.math.BigDecimal.ZERO, null));

        String created = mockMvc.perform(post("/api/creator/videos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID videoId = UUID.fromString(objectMapper.readTree(created).get("id").asText());

        mockMvc.perform(get("/api/videos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + videoId + "')]").exists());
        mockMvc.perform(get("/api/videos/{videoId}", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.videoUrl").value("https://cdn.example.com/stream.mp4"));
        mockMvc.perform(post("/api/videos/{videoId}/views", videoId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/videos/{videoId}/views", videoId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(1));
        mockMvc.perform(get("/api/videos/{videoId}", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(1));
        mockMvc.perform(get("/api/videos/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + videoId + "')]")
                        .value(org.hamcrest.Matchers.hasSize(1)));
        mockMvc.perform(get("/api/creator/videos/analytics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.videoCount").value(1))
                .andExpect(jsonPath("$.totalViews").value(1))
                .andExpect(jsonPath("$.topVideos[0].id").value(videoId.toString()));
    }

    @Test
    void recommendationsUseSharedViewingHistoryAndPopularity() throws Exception {
        var creatorOne = createUser("Recommender Creator One", "recommender-creator-one@example.com", UserRole.CREATOR);
        var creatorTwo = createUser("Recommender Creator Two", "recommender-creator-two@example.com", UserRole.CREATOR);
        var viewerOne = createUser("Recommender Viewer One", "recommender-viewer-one@example.com", UserRole.VIEWER);
        var viewerTwo = createUser("Recommender Viewer Two", "recommender-viewer-two@example.com", UserRole.VIEWER);
        var targetViewer = createUser("Target Recommender Viewer", "target-recommender@example.com", UserRole.VIEWER);
        UUID sharedVideo = publishVideo(creatorOne, "Shared viewing history");
        UUID suggestedVideo = publishVideo(creatorTwo, "Collaborative suggestion");

        recordView(sharedVideo, viewerOne);
        recordView(suggestedVideo, viewerOne);
        recordView(sharedVideo, viewerTwo);
        recordView(sharedVideo, targetViewer);

        mockMvc.perform(get("/api/feed/recommendations")
                        .header("Authorization", "Bearer " + jwtService.generateToken(targetViewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("hybrid-collaborative-popularity"))
                .andExpect(jsonPath("$.recommendations[0].id").value(suggestedVideo.toString()));
    }

    @Test
    void searchFindsVideosByTitleIgnoringCase() throws Exception {
        var creator = createUser("Search Creator", "search-creator@example.com", UserRole.CREATOR);
        UUID videoId = publishVideo(creator, "Nocturnal Andes Expedition");

        mockMvc.perform(get("/api/videos/search").param("q", "andes EXPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + videoId + "')]").exists());
        mockMvc.perform(get("/api/videos/search").param("q", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void profileEndpointReturnsAuthenticatedUser() throws Exception {
        var user = createUser("Profile Viewer", "profile-viewer@example.com", UserRole.VIEWER);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.displayName").value("Profile Viewer"))
                .andExpect(jsonPath("$.email").value("profile-viewer@example.com"))
                .andExpect(jsonPath("$.role").value("VIEWER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void creatorCanDeleteOnlyOwnVideos() throws Exception {
        var owner = createUser("Delete Owner", "delete-owner@example.com", UserRole.CREATOR);
        var otherCreator = createUser("Delete Other", "delete-other@example.com", UserRole.CREATOR);
        var viewer = createUser("Delete Viewer", "delete-viewer@example.com", UserRole.VIEWER);
        UUID videoId = publishVideo(owner, "Video to delete");
        recordView(videoId, viewer);

        mockMvc.perform(delete("/api/creator/videos/{videoId}", videoId)
                        .header("Authorization", "Bearer " + jwtService.generateToken(otherCreator)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/creator/videos/{videoId}", videoId)
                        .header("Authorization", "Bearer " + jwtService.generateToken(owner)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/videos/{videoId}", videoId))
                .andExpect(status().isNotFound());
    }

    @Test
    void recommendationsExcludeCreatorsOwnVideos() throws Exception {
        var creator = createUser("Self Feed Creator", "self-feed-creator@example.com", UserRole.CREATOR);
        UUID ownVideo = publishVideo(creator, "Own creator video");

        mockMvc.perform(get("/api/feed/recommendations")
                        .header("Authorization", "Bearer " + jwtService.generateToken(creator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[?(@.id == '" + ownVideo + "')]").isEmpty());
    }

        @Test
        void interactionDtoValidatesRatingAndWatchDuration() {
                var invalidInteraction = new CreateInteractionRequest(UUID.randomUUID(), 6, -1);

                var violations = validator.validate(invalidInteraction);

                org.junit.jupiter.api.Assertions.assertEquals(2, violations.size());
        }

    private com.kino.backend.model.User createUser(String name, String email, UserRole role) {
        return userRepository.save(new com.kino.backend.model.User(
                name, email, passwordEncoder.encode("secure-password-1"), role));
    }

    private UUID publishVideo(com.kino.backend.model.User creator, String title) throws Exception {
        String response = mockMvc.perform(post("/api/creator/videos")
                        .header("Authorization", "Bearer " + jwtService.generateToken(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateVideoRequest(
                                title, "Description for " + title, "https://cdn.example.com/" + title.hashCode() + ".mp4",
                                null, List.of("Documental"), java.math.BigDecimal.ZERO, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private void recordView(UUID videoId, com.kino.backend.model.User viewer) throws Exception {
        mockMvc.perform(post("/api/videos/{videoId}/views", videoId)
                        .header("Authorization", "Bearer " + jwtService.generateToken(viewer)))
                .andExpect(status().isOk());
    }
}