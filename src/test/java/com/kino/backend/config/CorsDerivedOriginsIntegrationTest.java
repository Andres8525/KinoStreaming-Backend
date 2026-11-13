package com.kino.backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Production configuration: only the main Vercel domain is configured, yet the frontend is also opened from
 * the deployment URL shown by Vercel's "Visit" button and from the local Angular dev server.
 */
@SpringBootTest(properties = "kino.cors.allowed-origins=kino-streamingg-frontend.vercel.app")
@AutoConfigureMockMvc
class CorsDerivedOriginsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsVercelDeploymentUrlsOfTheConfiguredProject() throws Exception {
        String deployment = "https://kino-streamingg-frontend-k3x9a1b2c-andres8525s-projects.vercel.app";
        preflight(deployment)
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", deployment));
    }

    @Test
    void allowsTheLocalAngularDevServer() throws Exception {
        preflight("http://localhost:4200")
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    void stillRejectsOtherVercelProjects() throws Exception {
        preflight("https://another-project.vercel.app").andExpect(status().isForbidden());
    }

    private ResultActions preflight(String origin) throws Exception {
        return mockMvc.perform(options("/api/auth/login")
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type"));
    }
}
