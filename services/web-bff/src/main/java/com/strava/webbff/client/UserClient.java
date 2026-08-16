package com.strava.webbff.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Calls user-service via Spring Cloud LoadBalancer (lb://user-service). The
 * caller's identity travels on the X-User-* headers attached by
 * {@link IdentityForwardingInterceptor}; admin calls therefore carry the
 * caller's ADMIN role and pass user-service's own admin guard.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserClient {

    private static final String BASE = "lb://user-service";

    private final RestTemplate loadBalanced;

    /** The dashboard's {@code user} section — UserResponse with counts, passed through verbatim. */
    @CircuitBreaker(name = "user-service", fallbackMethod = "currentUserFallback")
    @Retry(name = "user-service")
    public JsonNode currentUser() {
        return loadBalanced.getForObject(BASE + "/api/v1/users/me", JsonNode.class);
    }

    /** One page of the admin user list (HeaderResponse envelope with totals). */
    @CircuitBreaker(name = "user-service", fallbackMethod = "adminUsersFallback")
    @Retry(name = "user-service")
    public AdminPage adminUsers(int page, int size) {
        JsonNode envelope = loadBalanced.getForObject(
                BASE + "/api/v1/admin/users?page=" + page + "&size=" + size, JsonNode.class);
        return AdminPage.from(envelope);
    }

    /** Fallback: dashboard degrades to {@code "user": null} instead of failing the whole page. */
    @SuppressWarnings("unused")
    private JsonNode currentUserFallback(Throwable ex) {
        log.warn("user-service unavailable, dashboard user section degrades to null (cause: {})", ex.getMessage());
        return null;
    }

    /** Fallback: admin stats degrade to zero user totals instead of a 5xx admin console. */
    @SuppressWarnings("unused")
    private AdminPage adminUsersFallback(int page, int size, Throwable ex) {
        log.warn("user-service unavailable, admin user stats degrade to 0 (cause: {})", ex.getMessage());
        return AdminPage.EMPTY;
    }
}
