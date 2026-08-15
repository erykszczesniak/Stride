package com.strava.notificationservice.config

import com.strava.shared.security.HeaderAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/actuator/**").permitAll()
                    // Roles come from the gateway-injected X-User-Roles header
                    // (HeaderAuthenticationFilter maps them to ROLE_*).
                    .requestMatchers("/api/v1/notifications/weekly-summary/trigger").hasRole("ADMIN")
                    .anyRequest().authenticated()
            }
            .addFilterBefore(HeaderAuthenticationFilter(), UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
