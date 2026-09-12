package com.strava.backend.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Groq settings for goal / plan generation and the chat failover path,
 * bound from {@code groq.*} in application.yml. Like {@link ClaudeProperties},
 * an empty key keeps the app booting; the endpoints then answer 503.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "groq")
public class GroqProperties {

    private String apiKey = "";
    private String model = "openai/gpt-oss-120b";
    private String apiUrl = "https://api.groq.com/openai/v1/chat/completions";

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
