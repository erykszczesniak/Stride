package com.strava.activityservice.strava;

import lombok.Builder;
import lombok.Data;

/** Ported 1:1 from main — field names and JSON shape are the public contract. */
@Data
@Builder
public class StravaImportResponse {
    /** Human-readable confirmation that the job was accepted. Actual import runs async. */
    private String message;
    private boolean started;
}
