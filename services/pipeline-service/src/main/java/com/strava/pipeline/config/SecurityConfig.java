package com.strava.pipeline.config;

import com.strava.pipeline.security.PipelineTokenFilter;
import com.strava.pipeline.token.PipelineTokenService;
import com.strava.shared.security.HeaderAuthenticationFilter;
import com.strava.shared.security.JwtParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Two chains, mirroring main's SecurityConfiguration:
 *
 * 1. {@code /pipeline/v1/**} — reaches this service UNAUTHENTICATED (gateway
 *    public path, because tri_ personal tokens are not JWTs). The
 *    {@link PipelineTokenFilter} validates the bearer credential itself
 *    (tri_ token via the hashed store, or a Stride JWT via the shared
 *    secret); anything else is a plain 401.
 *
 * 2. everything else ({@code /api/v1/pipeline/tokens}) — standard service
 *    convention: the gateway already validated the JWT and injected
 *    X-User-* headers, translated by {@link HeaderAuthenticationFilter}.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public JwtParser jwtParser(@Value("${jwt.secret}") String secret) {
        return new JwtParser(secret);
    }

    /**
     * Dedicated chain for the pipeline facade: personal-token or JWT bearer,
     * no CORS (consumed by native clients). Ordered before the default chain
     * so the securityMatcher wins for /pipeline/v1/**.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain pipelineFilterChain(HttpSecurity http,
                                                   PipelineTokenService pipelineTokenService,
                                                   JwtParser jwtParser) throws Exception {
        http
                .securityMatcher("/pipeline/v1/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new PipelineTokenFilter(pipelineTokenService, jwtParser),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new HeaderAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
