package com.strava.backend.configuration;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Anthropic Claude settings for the AI activity-analysis chat. Populated from env vars via
 * {@code app.claude.*} in application.yml. The api-key default is empty on purpose — the app
 * still boots without it, but the chat endpoints will return 503 and a WARN is logged at startup.
 */
@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.claude")
public class ClaudeProperties {

    private String apiKey = "";
    /** Model id — defaults to Claude Opus 4.8, the most capable Opus-tier model. */
    private String model = "claude-opus-4-8";
    /** Upper bound on tokens generated per reply. Keeps a single analysis snappy and bounded. */
    private long maxTokens = 1024;

    /** Returns true only when an api key is present. */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @PostConstruct
    void warnIfUnconfigured() {
        if (!isConfigured()) {
            log.warn("Claude AI chat is not configured — set ANTHROPIC_API_KEY to enable it. "
                    + "Endpoints under /api/v1/ai/activities/**/chat will return 503.");
        } else {
            log.info("Claude AI chat configured (model={}, maxTokens={})", model, maxTokens);
        }
    }
}
