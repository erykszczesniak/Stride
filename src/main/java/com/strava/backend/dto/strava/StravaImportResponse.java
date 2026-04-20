package com.strava.backend.dto.strava;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StravaImportResponse {
    /** Human-readable confirmation that the job was accepted. Actual import runs async. */
    private String message;
    private boolean started;
}
