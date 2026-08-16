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

import java.util.ArrayList;
import java.util.List;

/**
 * Calls activity-service via Spring Cloud LoadBalancer (lb://activity-service),
 * identity forwarded on X-User-* headers. Dashboard sections are passed through
 * verbatim as JSON trees so the wire shape stays byte-compatible with the
 * monolith; the report path uses the typed {@link ActivityView} instead because
 * it derives values rather than re-emitting the payload.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityClient {

    private static final String BASE = "lb://activity-service";
    /** Page size for the report's full-history fetch. */
    static final int REPORT_PAGE_SIZE = 200;
    /** Hard cap so a runaway history cannot turn one report into hundreds of calls. */
    static final int REPORT_MAX_PAGES = 25;

    private static final ParameterizedTypeReference<List<JsonNode>> JSON_LIST =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<ActivityView>> ACTIVITY_LIST =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate loadBalanced;

    /** The dashboard's {@code stats} section (monthly ActivityStatsResponse), verbatim. */
    @CircuitBreaker(name = "activity-service", fallbackMethod = "nodeFallback")
    @Retry(name = "activity-service")
    public JsonNode monthlyStats() {
        return loadBalanced.getForObject(BASE + "/api/v1/activities/stats/monthly", JsonNode.class);
    }

    /** The dashboard's {@code weeklyStats} section (WeeklyStatsResponse), verbatim. */
    @CircuitBreaker(name = "activity-service", fallbackMethod = "nodeFallback")
    @Retry(name = "activity-service")
    public JsonNode weeklyStats() {
        return loadBalanced.getForObject(BASE + "/api/v1/activities/stats/weekly", JsonNode.class);
    }

    /**
     * Last {@code limit} activities, newest first — activity-service's /my list is
     * ordered createdAt DESC and already enriched with social fields, matching the
     * monolith's {@code getRecentActivitiesByUserId(userId, 5)}.
     */
    @CircuitBreaker(name = "activity-service", fallbackMethod = "recentActivitiesFallback")
    @Retry(name = "activity-service")
    public List<JsonNode> recentActivities(int limit) {
        ResponseEntity<List<JsonNode>> response = loadBalanced.exchange(
                BASE + "/api/v1/activities/my?page=0&size=" + limit,
                HttpMethod.GET, HttpEntity.EMPTY, JSON_LIST);
        return response.getBody() != null ? response.getBody() : List.of();
    }

    /**
     * The caller's full activity history for the report builder, following the
     * page-total-count header until exhausted (capped at {@link #REPORT_MAX_PAGES}).
     */
    @CircuitBreaker(name = "activity-service", fallbackMethod = "allMyActivitiesFallback")
    @Retry(name = "activity-service")
    public List<ActivityView> allMyActivities() {
        List<ActivityView> all = new ArrayList<>();
        int page = 0;
        int totalPages = 1;
        while (page < totalPages && page < REPORT_MAX_PAGES) {
            ResponseEntity<List<ActivityView>> response = loadBalanced.exchange(
                    BASE + "/api/v1/activities/my?page=" + page + "&size=" + REPORT_PAGE_SIZE,
                    HttpMethod.GET, HttpEntity.EMPTY, ACTIVITY_LIST);
            List<ActivityView> body = response.getBody();
            if (body == null || body.isEmpty()) {
                break;
            }
            all.addAll(body);
            String header = response.getHeaders().getFirst("page-total-count");
            totalPages = header != null ? Integer.parseInt(header) : page + 1;
            page++;
        }
        return all;
    }

    /** One page of the admin activity list (HeaderResponse envelope with totals). */
    @CircuitBreaker(name = "activity-service", fallbackMethod = "adminActivitiesFallback")
    @Retry(name = "activity-service")
    public AdminPage adminActivities(int page, int size) {
        JsonNode envelope = loadBalanced.getForObject(
                BASE + "/api/v1/admin/activities?page=" + page + "&size=" + size, JsonNode.class);
        return AdminPage.from(envelope);
    }

    /** Fallback: stats sections degrade to null instead of failing the dashboard. */
    @SuppressWarnings("unused")
    private JsonNode nodeFallback(Throwable ex) {
        log.warn("activity-service unavailable, stats section degrades to null (cause: {})", ex.getMessage());
        return null;
    }

    /** Fallback: the dashboard shows an empty recent-activities list. */
    @SuppressWarnings("unused")
    private List<JsonNode> recentActivitiesFallback(int limit, Throwable ex) {
        log.warn("activity-service unavailable, recent activities degrade to [] (cause: {})", ex.getMessage());
        return List.of();
    }

    /** Fallback: the report renders with zero rows instead of a 5xx. */
    @SuppressWarnings("unused")
    private List<ActivityView> allMyActivitiesFallback(Throwable ex) {
        log.warn("activity-service unavailable, report degrades to empty rows (cause: {})", ex.getMessage());
        return List.of();
    }

    /** Fallback: admin stats degrade to zero activity totals instead of a 5xx admin console. */
    @SuppressWarnings("unused")
    private AdminPage adminActivitiesFallback(int page, int size, Throwable ex) {
        log.warn("activity-service unavailable, admin activity stats degrade to 0 (cause: {})", ex.getMessage());
        return AdminPage.EMPTY;
    }
}
