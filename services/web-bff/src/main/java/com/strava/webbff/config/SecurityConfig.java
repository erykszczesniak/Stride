package com.strava.webbff.config;

import com.strava.shared.security.HeaderAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Same trust model as every other service behind the gateway: the gateway has
 * already validated the JWT and injected X-User-Id / X-User-Email / X-User-Roles;
 * {@link HeaderAuthenticationFilter} turns those into an Authentication.
 * {@code /api/v1/admin/**} additionally requires ROLE_ADMIN — mirroring the
 * monolith's admin guard on {@code /api/v1/admin/stats}.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new HeaderAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
