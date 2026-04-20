package com.strava.backend.dto.strava;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StravaStatusResponse {
    private boolean connected;
    private Long athleteId;
    private LocalDateTime connectedAt;
    private LocalDateTime lastImportedAt;
    private long activityCount;
}
