package com.strava.backend.dto.importactivity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
