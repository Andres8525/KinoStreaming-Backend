package com.kino.backend.config;

import com.kino.backend.security.CustomUserDetailsService;
import com.kino.backend.security.JsonAccessDeniedHandler;
import com.kino.backend.security.JsonAuthenticationEntryPoint;
import com.kino.backend.security.JwtAuthenticationFilter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
    private static final List<String> ALLOWED_HEADERS = List.of("Authorization", "Content-Type", "Accept");
    private static final List<String> EXPOSED_HEADERS = List.of("Authorization");

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfig.class);

    private final CorsOrigins corsOrigins;

    public SecurityConfig(
            @Value("${kino.cors.allowed-origins:http://localhost:4200}") String allowedOrigins,
            @Value("${kino.cors.allow-vercel-previews:true}") boolean allowVercelPreviews,
            @Value("${kino.cors.allow-localhost:true}") boolean allowLocalhost) {
        this.corsOrigins = CorsOrigins.parse(allowedOrigins).withDerivedOrigins(allowVercelPreviews, allowLocalhost);
        if (corsOrigins.isEmpty()) {
            LOGGER.warn("CORS_ALLOWED_ORIGINS is empty: browsers on other domains will not be able to call the API");
        } else {
            LOGGER.info("CORS allowed origins: {} patterns: {}", corsOrigins.origins(), corsOrigins.patterns());
        }
    }

    /** The parsed CORS_ALLOWED_ORIGINS, shared with the health endpoint for diagnostics. */
    @Bean
    CorsOrigins corsOrigins() {
        return corsOrigins;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsOrigins.origins());
        configuration.setAllowedOriginPatterns(corsOrigins.patterns());
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setExposedHeaders(EXPOSED_HEADERS);
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(CustomUserDetailsService userDetailsService,
                                                 PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
                                            JsonAuthenticationEntryPoint authenticationEntryPoint,
                                            JsonAccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/videos", "/api/videos/**").permitAll()
                        // Watching a camera broadcast (WebRTC signaling) requires an account.
                        .requestMatchers("/api/live/*/viewers", "/api/live/*/viewers/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/live", "/api/live/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ai/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/videos/*/views").authenticated()
                        // Every signed-in account can publish and go live; ownership is checked per resource.
                        .requestMatchers("/api/creator/**").authenticated()
                        .requestMatchers("/api/feed/**").authenticated()
                        .requestMatchers("/api/users/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}