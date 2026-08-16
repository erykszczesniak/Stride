package com.strava.activityservice.config;

import com.strava.shared.security.HeaderAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
                        // Static photo serving — public in main too.
                        .requestMatchers("/uploads/**").permitAll()
                        // Cluster-internal API: called service-to-service without
                        // user identity headers. The gateway has no /internal route,
                        // so it is unreachable from outside the cluster; k8s
                        // NetworkPolicies narrow it further.
                        .requestMatchers("/internal/**").permitAll()
                        // Strava's OAuth redirect hits this without our JWT or the
                        // gateway's identity headers — identity is recovered from
                        // the HMAC-signed state parameter (mirrors main's permitAll).
                        .requestMatchers("/api/v1/integrations/strava/callback").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new HeaderAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
