package com.strava.userservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Enables Spring Data Elasticsearch repositories scoped to the search package. The
 * client connection itself is configured via {@code spring.elasticsearch.uris}.
 * Disabled in the {@code standalone} profile (H2, no ES) — user search then serves the
 * SQL fallback only.
 */
@Configuration
@Profile("!standalone")
@EnableElasticsearchRepositories(basePackages = "com.strava.userservice.search")
public class ElasticsearchConfig {
}
