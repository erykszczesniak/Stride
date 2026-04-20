package com.strava.backend.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Explicitly enables Spring Data Elasticsearch repositories scoped to the
 * {@code com.strava.backend.search} package. The client connection itself is
 * configured via {@code spring.elasticsearch.uris} and Spring Boot
 * auto-configuration.
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.strava.backend.search")
public class ElasticsearchConfiguration {
}
