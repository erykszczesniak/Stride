package com.strava.activityservice.strava;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Ported 1:1 from main — field names and JSON shape are the public contract. */
@Data
@Builder
public class StravaStatusResponse {
    private boolean connected;
    private Long athleteId;
    private LocalDateTime connectedAt;
    private LocalDateTime lastImportedAt;
    private long activityCount;
}
