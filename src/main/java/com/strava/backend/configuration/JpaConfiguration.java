package com.strava.backend.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Explicitly scopes Spring Data JPA repository scanning to the
 * {@code com.strava.backend.repository} package. Without this the JPA module
 * also inspects {@code com.strava.backend.search} (Elasticsearch repositories)
 * and logs a "could not safely identify store assignment" notice for each.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.strava.backend.repository")
public class JpaConfiguration {
}
