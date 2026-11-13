package com.kino.backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** CORS with a value typed the way it often is in a hosting dashboard (quotes, trailing slash, ';', pattern). */
@SpringBootTest(properties = "kino.cors.allowed-origins= \"https://kino-frontend.vercel.app/\" ; https://kino-frontend-*.vercel.app")
@AutoConfigureMockMvc
class CorsConfigurationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsTheProductionFrontendDespiteFormatting() throws Exception {
        preflight("https://kino-frontend.vercel.app")
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://kino-frontend.vercel.app"));
    }

    @Test
    void allowsPreviewDeploymentsMatchingThePattern() throws Exception {
        preflight("https://kino-frontend-git-feature-andres.vercel.app")
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://kino-frontend-git-feature-andres.vercel.app"));
    }

    @Test
    void rejectsOtherOrigins() throws Exception {
        preflight("https://evil.example.com").andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.ResultActions preflight(String origin) throws Exception {
        return mockMvc.perform(options("/api/auth/login")
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type"));
    }
}
