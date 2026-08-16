package com.strava.webbff.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * One page of an admin list endpoint. Those endpoints serialize the monolith's
 * {@code HeaderResponse} envelope as the whole body:
 * {@code {"items":[...],"headers":{"page-total-count":["1"],"page-total-elements":["42"]}}}
 * — the totals let the BFF report exact totalUsers/totalActivities without
 * fetching everything.
 */
public record AdminPage(List<JsonNode> items, int totalPages, long totalElements) {

    public static final AdminPage EMPTY = new AdminPage(List.of(), 0, 0);

    public static AdminPage from(JsonNode envelope) {
        if (envelope == null) {
            return EMPTY;
        }
        List<JsonNode> items = new ArrayList<>();
        envelope.path("items").forEach(items::add);
        int totalPages = envelope.path("headers").path("page-total-count").path(0).asInt(0);
        long totalElements = envelope.path("headers").path("page-total-elements").path(0).asLong(0);
        return new AdminPage(items, totalPages, totalElements);
    }
}
