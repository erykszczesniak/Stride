package com.strava.backend.dto.importactivity;

import com.strava.backend.dto.activity.ActivityResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
