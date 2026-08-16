package com.strava.activityservice.importer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GpsPoint {
    private double latitude;
    private double longitude;
    private Double elevation;
    private LocalDateTime time;
    private Integer heartRate;
    private Integer cadence;
    private Integer power;
    private Double distanceMeters;
}
