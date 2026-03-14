package com.strava.backend.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfiguration implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("page-total-count", "page-total-elements", "Authorization")
                .allowCredentials(true)
                .maxAge(3600);

        // GraphQL endpoints - use same allowed origins as REST API
        registry.addMapping("/graphql/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // API endpoints configuration
        org.springframework.web.cors.CorsConfiguration apiConfig = new org.springframework.web.cors.CorsConfiguration();
        apiConfig.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        apiConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        apiConfig.setAllowedHeaders(List.of("*"));
        apiConfig.setExposedHeaders(Arrays.asList("page-total-count", "page-total-elements", "Authorization"));
        apiConfig.setAllowCredentials(true);
        apiConfig.setMaxAge(3600L);
        source.registerCorsConfiguration("/api/**", apiConfig);

        // GraphQL endpoints - use same allowed origins as REST API
        org.springframework.web.cors.CorsConfiguration graphqlConfig = new org.springframework.web.cors.CorsConfiguration();
        graphqlConfig.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        graphqlConfig.setAllowedMethods(Arrays.asList("GET", "POST", "OPTIONS"));
        graphqlConfig.setAllowedHeaders(List.of("*"));
        graphqlConfig.setAllowCredentials(true);
        graphqlConfig.setMaxAge(3600L);
        source.registerCorsConfiguration("/graphql/**", graphqlConfig);

        return source;
    }
}
