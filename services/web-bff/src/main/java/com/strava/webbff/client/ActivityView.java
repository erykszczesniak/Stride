package com.strava.webbff.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * Typed view of an {@code ActivityResponse} from activity-service — only the
 * fields the report builder / CSV export derive values from. Unknown fields
 * (photos, gear, elevation profile, ...) are ignored on purpose: the report
 * never re-emits the raw activity, so no passthrough fidelity is lost.
 *
 * <p>{@code likesCount}/{@code commentsCount} arrive already enriched with the
 * social counts, which is what the monolith read off the entity graph.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ActivityView(
        Long id,
        String title,
        String activityType,
        Double distance,
        Integer duration,
        Integer elevationGain,
        Integer calories,
        Double averageSpeed,
        Integer averageHeartRate,
        LocalDateTime startTime,
        LocalDateTime createdAt,
        int likesCount,
        int commentsCount
) {
}
