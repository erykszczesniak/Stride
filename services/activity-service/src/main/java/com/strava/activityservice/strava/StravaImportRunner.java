package com.strava.activityservice.strava;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.activityservice.activity.ActivityRequest;
import com.strava.activityservice.activity.ActivityService;
import com.strava.activityservice.domain.Activity;
import com.strava.activityservice.domain.StravaConnection;
import com.strava.activityservice.domain.StravaConnectionRepository;
import com.strava.activityservice.domain.StravaImportedActivity;
import com.strava.activityservice.domain.StravaImportedActivityRepository;
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
 * {@link StravaIntegrationService} so Spring's proxying picks up the async entry point.
 * Each imported activity goes through {@link ActivityService#createActivity} — the
 * same pipeline as POST /api/v1/activities (PR detection + Kafka activity-created event).
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
    private final StravaProperties stravaProperties;

    /** Instantiated here rather than injected, matching the style in StravaIntegrationService. */
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
                        ActivityRequest req = integrationService.toActivityRequest(node);

                        // Fetch the streams endpoint for this activity to pull real GPS
                        // coordinates, per-point timestamps, and elevation. Without this the
                        // Splits/map/elevation charts fall back to uniform fills. A failure
                        // here (404 for manual activities, 429 rate limit, network blip) is
                        // non-fatal: we warn and persist the summary row without GPS.
                        if (stravaProperties.isImportStreams()) {
                            JsonNode streams = fetchStreams(accessToken, stravaId);
                            if (streams != null) {
                                integrationService.applyStreamsToRequest(req, streams, req.getStartTime());
                            }
                        }

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

    /**
     * Fetch the latlng+time+altitude+heartrate streams for a single Strava activity.
     * Counts against the same 100 req / 15 min rate-limit budget as the activity-list
     * call, so we run every request through {@link StravaRateLimiter}.
     * <p>
     * Returns {@code null} (and logs a warn) on any non-2xx — the caller persists the
     * summary row without GPS rather than aborting the whole import. This includes:
     * <ul>
     *   <li>404 — manual activities without GPS, or activities made private by the athlete</li>
     *   <li>429 — rate limited (we also notify the limiter so subsequent calls back off)</li>
     *   <li>401 — expired token mid-import (rare; token is refreshed at import start)</li>
     *   <li>5xx / timeouts — transient Strava-side failures</li>
     * </ul>
     */
    JsonNode fetchStreams(String accessToken, long activityId) {
        String url = String.format(StravaIntegrationService.STREAMS_URL_TEMPLATE, activityId);
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
            log.warn("Strava 429 on streams for activity {} — skipping streams, keeping summary",
                    activityId);
            return null;
        } catch (HttpClientErrorException e) {
            // 404 is the common case (manual activities without GPS); don't spam at ERROR.
            log.warn("Strava streams {} for activity {} — importing without GPS",
                    e.getStatusCode(), activityId);
            return null;
        } catch (Exception e) {
            log.warn("Strava streams fetch failed for activity {}: {} — importing without GPS",
                    activityId, e.getMessage());
            return null;
        }
    }

    private JsonNode fetchPage(String accessToken, int page) {
        String url = UriComponentsBuilder.fromHttpUrl(StravaIntegrationService.ACTIVITIES_URL)
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
