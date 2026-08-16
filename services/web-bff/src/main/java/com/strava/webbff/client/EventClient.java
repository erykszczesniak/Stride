package com.strava.webbff.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Calls club-event-service via Spring Cloud LoadBalancer
 * (lb://club-event-service), identity forwarded on X-User-* headers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventClient {

    private static final String BASE = "lb://club-event-service";

    private static final ParameterizedTypeReference<List<JsonNode>> JSON_LIST =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate loadBalanced;

    /**
     * Upcoming events the caller is registered for, soonest first, with
     * registeredCount/isRegistered — same query the monolith's dashboard used
     * ({@code findUpcomingEventsByUserId}); the BFF slices to the first 3.
     */
    @CircuitBreaker(name = "club-event-service", fallbackMethod = "listFallback")
    @Retry(name = "club-event-service")
    public List<JsonNode> myUpcomingEvents() {
        return list(BASE + "/api/v1/events/my");
    }

    /** Upcoming public events (first {@code size}) — used only to approximate the admin total. */
    @CircuitBreaker(name = "club-event-service", fallbackMethod = "sizedListFallback")
    @Retry(name = "club-event-service")
    public List<JsonNode> upcomingPublicEvents(int size) {
        return list(BASE + "/api/v1/events?page=0&size=" + size);
    }

    /** Clubs (first {@code size}) — used only to approximate the admin total. */
    @CircuitBreaker(name = "club-event-service", fallbackMethod = "sizedListFallback")
    @Retry(name = "club-event-service")
    public List<JsonNode> clubs(int size) {
        return list(BASE + "/api/v1/clubs?page=0&size=" + size);
    }

    private List<JsonNode> list(String url) {
        ResponseEntity<List<JsonNode>> response =
                loadBalanced.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, JSON_LIST);
        return response.getBody() != null ? response.getBody() : List.of();
    }

    /** Fallback: the dashboard shows no upcoming events instead of failing. */
    @SuppressWarnings("unused")
    private List<JsonNode> listFallback(Throwable ex) {
        log.warn("club-event-service unavailable, events degrade to [] (cause: {})", ex.getMessage());
        return List.of();
    }

    /** Fallback: admin club/event counts degrade to 0. */
    @SuppressWarnings("unused")
    private List<JsonNode> sizedListFallback(int size, Throwable ex) {
        log.warn("club-event-service unavailable, admin count degrades to 0 (cause: {})", ex.getMessage());
        return List.of();
    }
}
