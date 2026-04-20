package com.strava.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.domain.Activity;
import com.strava.backend.domain.StravaConnection;
import com.strava.backend.domain.StravaImportedActivity;
import com.strava.backend.dto.activity.ActivityRequest;
import com.strava.backend.repository.StravaConnectionRepository;
import com.strava.backend.repository.StravaImportedActivityRepository;
import com.strava.backend.service.ActivityService;
import com.strava.backend.service.StravaIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Runs the (potentially long) import loop on a dedicated Spring {@code @Async} thread
 * so the HTTP /import call can return 202 immediately. Separated from
 * {@link StravaIntegrationServiceImpl} so Spring's proxying picks up the async entry point.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StravaImportRunner {

    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 50;

    private final StravaIntegrationService integrationService;
    private final StravaConnectionRepository connectionRepository;
    private final StravaImportedActivityRepository importedRepository;
    private final ActivityService activityService;
    private final StravaRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    /** Instantiated here rather than injected, matching the style in StravaIntegrationServiceImpl. */
    private RestTemplate restTemplate = new RestTemplate();

    @Async
    public void run(Long userId) {
        log.info("Starting Strava import for user {}", userId);
        int imported = 0;
        int skipped = 0;
        try {
            String accessToken = integrationService.refreshTokenIfNeeded(userId);
            Set<Long> seen = new HashSet<>(importedRepository.findStravaActivityIdsByUserId(userId));

            for (int page = 1; page <= MAX_PAGES; page++) {
                JsonNode batch = fetchPage(accessToken, page);
                if (batch == null || !batch.isArray() || batch.isEmpty()) break;

                for (JsonNode node : batch) {
                    long stravaId = node.path("id").asLong(-1);
                    if (stravaId <= 0) continue;
                    if (seen.contains(stravaId)
                            || importedRepository.existsByStravaActivityIdAndUserId(stravaId, userId)) {
                        skipped++;
                        continue;
                    }
                    try {
                        ActivityRequest req =
                                ((StravaIntegrationServiceImpl) integrationService).toActivityRequest(node);
                        Activity created = activityService.createActivity(userId, req);
                        importedRepository.save(StravaImportedActivity.builder()
                                .stravaActivityId(stravaId)
                                .userId(userId)
                                .activityId(created.getId())
                                .build());
                        seen.add(stravaId);
                        imported++;
                    } catch (Exception e) {
                        log.warn("Skipping Strava activity {} for user {}: {}",
                                stravaId, userId, e.getMessage());
                    }
                }
                if (batch.size() < PAGE_SIZE) break;
            }

            Optional<StravaConnection> conn = connectionRepository.findByUserId(userId);
            conn.ifPresent(c -> {
                c.setLastImportedAt(LocalDateTime.now());
                connectionRepository.save(c);
            });
            log.info("Strava import done for user {}: imported={}, skipped={}", userId, imported, skipped);
        } catch (Exception e) {
            log.error("Strava import failed for user {}: {}", userId, e.getMessage(), e);
        }
    }

    private JsonNode fetchPage(String accessToken, int page) {
        String url = UriComponentsBuilder.fromHttpUrl(StravaIntegrationServiceImpl.ACTIVITIES_URL)
                .queryParam("page", page)
                .queryParam("per_page", PAGE_SIZE)
                .build()
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            rateLimiter.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return null;
        }

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (HttpClientErrorException.TooManyRequests e429) {
            String h = e429.getResponseHeaders() == null
                    ? null : e429.getResponseHeaders().getFirst("Retry-After");
            long retryAfter;
            try {
                retryAfter = h == null ? 60 : Long.parseLong(h);
            } catch (NumberFormatException nfe) {
                retryAfter = 60;
            }
            rateLimiter.notify429(retryAfter);
            log.warn("Strava returned 429 on page {} — stopping this import run", page);
            return null;
        } catch (Exception e) {
            log.error("Strava activities fetch failed on page {}: {}", page, e.getMessage());
            return null;
        }
    }

    // Test seam.
    void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
