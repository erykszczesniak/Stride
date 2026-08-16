package com.strava.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds the {@code jwt.*} path lists. A {@code @ConfigurationProperties} binder is
 * required here: the values arrive from Config Server as YAML lists (indexed keys
 * {@code jwt.public-paths[0]}…), which a flat {@code @Value("${jwt.public-paths}")}
 * placeholder silently resolves to its empty default.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtFilterProperties(
        String secret,
        List<String> publicPaths,
        List<String> queryTokenPaths
) {
    public JwtFilterProperties {
        publicPaths = publicPaths != null ? publicPaths : List.of();
        queryTokenPaths = queryTokenPaths != null ? queryTokenPaths : List.of();
    }
}
