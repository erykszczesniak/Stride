package com.strava.pipeline.client;

import com.strava.shared.security.UserContext;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The user's upcoming registered events from club-event-service
 * (GET /api/v1/events/my — the microservice port of main's
 * eventService.getUpcomingEventsByUserId, which names the pipeline's season
 * goal). On failure the pipeline degrades to the "Season {year}" placeholder
 * goal, exactly like a user with no upcoming events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventClient {

    private final RestTemplate loadBalanced;

    public record EventData(String name, LocalDateTime eventDate) {
    }

    @CircuitBreaker(name = "club-event-service", fallbackMethod = "eventsFallback")
    @Retry(name = "club-event-service")
    public List<EventData> myUpcomingEvents(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(UserContext.HEADER_USER_ID, userId.toString());
        var response = loadBalanced.exchange("lb://club-event-service/api/v1/events/my",
                HttpMethod.GET, new HttpEntity<>(headers),
                new ParameterizedTypeReference<List<EventData>>() {
                });
        List<EventData> body = response.getBody();
        return body == null ? List.of() : body;
    }

    @SuppressWarnings("unused")
    private List<EventData> eventsFallback(Long userId, Throwable ex) {
        log.warn("EventClient fallback for userId={} (cause: {})", userId, ex.getMessage());
        return List.of();
    }
}
