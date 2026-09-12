package com.strava.backend.ai.guard;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Guardrail settings for every LLM-backed endpoint, bound from {@code app.ai.*}.
 * Defaults are the production values. Same keys as the {@code microservices}
 * branch so one env file configures both.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.ai")
public class AiGuardProperties {

    private RateLimit rateLimit = new RateLimit();
    private Guard guard = new Guard();
    private Pii pii = new Pii();
    private Failover failover = new Failover();

    /** USD per million tokens, keyed by model id as the provider reports it. */
    private Map<String, ModelPrice> pricing = new HashMap<>();

    @Getter
    @Setter
    public static class Failover {
        /** Route chat to Groq when Anthropic is unavailable (502/503, no key). */
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class RateLimit {
        /** Chat turns one user may send per minute (persisted, provider-billed). */
        private int chatPerMinute = 10;

        /** Goal / plan generations one user may request per minute. */
        private int generationPerMinute = 5;
    }

    @Getter
    @Setter
    public static class Guard {
        private boolean enabled = true;

        /** Longest run of the same character tolerated before the message is treated as noise. */
        private int maxRepeatedChars = 40;
    }

    @Getter
    @Setter
    public static class Pii {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class ModelPrice {
        private double inputPerMtok;
        private double outputPerMtok;
    }

    /** Price entry for {@code model}, or null when the model is not in the table. */
    public ModelPrice priceFor(String model) {
        return pricing.get(model);
    }
}
