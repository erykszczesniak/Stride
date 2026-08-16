package com.strava.activityservice.importactivity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ported 1:1 from main — field names and JSON shape are the public contract. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleFileInfo {
    private String filename;
    private String format;
    private String activityType;
    private String description;
    private String downloadUrl;
}
