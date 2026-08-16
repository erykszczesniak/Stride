package com.strava.activityservice.importactivity;

import com.strava.activityservice.activity.ActivityResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ported 1:1 from main — field names and JSON shape are the public contract. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResponse {
    private ActivityResponse activity;
    private String sourceFormat;
    private String sourceFilename;
    private int gpsPointsImported;
}
