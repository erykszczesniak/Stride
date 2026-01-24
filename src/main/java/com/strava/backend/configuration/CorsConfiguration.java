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

        // GraphQL endpoints - allow all origins for development
        registry.addMapping("/graphql/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);

        registry.addMapping("/graphiql/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
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

        // GraphQL endpoints - allow all origins for development/testing
        org.springframework.web.cors.CorsConfiguration graphqlConfig = new org.springframework.web.cors.CorsConfiguration();
        graphqlConfig.setAllowedOriginPatterns(List.of("*"));
        graphqlConfig.setAllowedMethods(Arrays.asList("GET", "POST", "OPTIONS"));
        graphqlConfig.setAllowedHeaders(List.of("*"));
        graphqlConfig.setAllowCredentials(true);
        graphqlConfig.setMaxAge(3600L);
        source.registerCorsConfiguration("/graphql/**", graphqlConfig);
        source.registerCorsConfiguration("/graphiql/**", graphqlConfig);

        return source;
    }
}
