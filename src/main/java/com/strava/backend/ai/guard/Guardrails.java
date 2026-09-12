package com.strava.backend.ai.guard;

import com.strava.backend.exception.ApiRequestException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * The single entry point the service layer calls before any text reaches a
 * provider: rate limit → prompt guard → PII masking. Order matters — a
 * rate-limited user must not get free guard evaluations, and the guard must
 * see the original text (masking could hide an injection inside an email
 * local part). Every outcome is a metric with bounded labels.
 */
@Slf4j
@Component
public class Guardrails {

    public enum Endpoint {
        CHAT("chat"),
        GENERATION("generation");

        private final String bucket;

        Endpoint(String bucket) {
            this.bucket = bucket;
        }

        public String bucket() {
            return bucket;
        }
    }

    /** Requests without a resolvable principal share one budget instead of escaping the limit. */
    static final long ANONYMOUS = -1L;

    private final AiGuardProperties properties;
    private final MeterRegistry meterRegistry;
    private final AiRateLimiter rateLimiter;
    private final PromptGuard promptGuard;
    private final PiiMasker piiMasker;

    public Guardrails(AiGuardProperties properties, MeterRegistry meterRegistry, AiRateLimiter rateLimiter,
                      PromptGuard promptGuard, PiiMasker piiMasker) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.rateLimiter = rateLimiter;
        this.promptGuard = promptGuard;
        this.piiMasker = piiMasker;
    }

    /**
     * Admits {@code text} from {@code userId} for {@code endpoint} and returns
     * the version that may be sent to a provider and persisted (PII masked).
     *
     * @throws ApiRequestException 429 when the user is over their per-minute
     *         budget, 400 when the guard rejects the text.
     */
    public String admit(Long userId, Endpoint endpoint, String text) {
        long subject = userId == null ? ANONYMOUS : userId;
        int limit = switch (endpoint) {
            case CHAT -> properties.getRateLimit().getChatPerMinute();
            case GENERATION -> properties.getRateLimit().getGenerationPerMinute();
        };
        if (!rateLimiter.tryAcquire(endpoint.bucket(), subject, limit)) {
            meterRegistry.counter("ai.rate_limited", "endpoint", endpoint.bucket()).increment();
            log.info("ai.guard rate_limited endpoint={} userId={}", endpoint.bucket(), subject);
            throw new ApiRequestException(
                    "Too many AI requests — limit is " + limit + " per minute for " + endpoint.bucket(),
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        if (properties.getGuard().isEnabled()) {
            PromptGuard.Decision decision = promptGuard.check(text);
            if (decision instanceof PromptGuard.Decision.Blocked blocked) {
                meterRegistry.counter("ai.guard.decisions", "decision", "blocked",
                        "reason", blocked.reason().name().toLowerCase(Locale.ROOT)).increment();
                // Never log the text itself: it is user content and may contain PII.
                log.info("ai.guard blocked endpoint={} userId={} reason={} length={}",
                        endpoint.bucket(), subject, blocked.reason(), text.length());
                throw new ApiRequestException(blocked.reason().userMessage(), HttpStatus.BAD_REQUEST);
            }
            meterRegistry.counter("ai.guard.decisions", "decision", "allowed", "reason", "none").increment();
        }

        if (!properties.getPii().isEnabled()) return text;
        PiiMasker.Result masked = piiMasker.mask(text);
        masked.counts().forEach((type, n) ->
                meterRegistry.counter("ai.pii.masked", "type", type.name().toLowerCase(Locale.ROOT)).increment(n));
        if (masked.total() > 0) {
            log.info("ai.guard pii_masked endpoint={} userId={} counts={}", endpoint.bucket(), subject, masked.counts());
        }
        return masked.text();
    }
}
