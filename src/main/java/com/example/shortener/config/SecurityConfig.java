package com.example.shortener.config;

import com.example.shortener.util.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Stateless API security. Callers authenticate with a Firebase ID token
 * ({@code Authorization: Bearer <token>}); the token signature, issuer,
 * audience and expiry are verified against Google's public keys.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Google's JWK set for Firebase ID tokens. */
    static final String FIREBASE_JWK_SET_URI =
            "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // no cookies: bearer tokens only
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error", "/api/v1/health", "/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        // Anonymous visitors may create links and report abuse.
                        .requestMatchers(HttpMethod.POST, "/api/v1/urls/shorten", "/api/v1/reports").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        // Short-link redirects: a single path segment at the root.
                        .requestMatchers(HttpMethod.GET, "/*").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/*").permitAll()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(jsonEntryPoint()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jsonEntryPoint())
                        .accessDeniedHandler(jsonAccessDenied()));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(AppProperties props) {
        String projectId = props.getFirebaseProjectId();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(FIREBASE_JWK_SET_URI).build();
        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<Collection<String>>(
                JwtClaimNames.AUD, aud -> aud != null && aud.contains(projectId));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer("https://securetoken.google.com/" + projectId),
                audience));
        return decoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(props.getCors().getAllowedOrigins().stream()
                .map(String::trim).filter(s -> !s.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private AuthenticationEntryPoint jsonEntryPoint() {
        return (request, response, ex) ->
                writeError(response, HttpStatus.UNAUTHORIZED, "Sign in to continue.");
    }

    private AccessDeniedHandler jsonAccessDenied() {
        return (request, response, ex) ->
                writeError(response, HttpStatus.FORBIDDEN, "You do not have access to this resource.");
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(message, status.value()));
    }
}
