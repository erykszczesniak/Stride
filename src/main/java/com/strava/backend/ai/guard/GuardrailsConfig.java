package com.strava.backend.ai.guard;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Explicit beans for the guard building blocks (plain classes, no Spring annotations of their own). */
@Configuration
public class GuardrailsConfig {

    @Bean
    public AiRateLimiter aiRateLimiter() {
        return new AiRateLimiter();
    }

    @Bean
    public PromptGuard promptGuard(AiGuardProperties properties) {
        return new PromptGuard(properties.getGuard().getMaxRepeatedChars());
    }

    @Bean
    public PiiMasker piiMasker() {
        return new PiiMasker();
    }
}
