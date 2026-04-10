package com.strava.backend.service.importer;

import com.strava.backend.enums.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedActivity {
    private String title;
    private String description;
    private ActivityType activityType;
    private Double distance;        // kilometers
    private Integer duration;       // seconds
    private Integer elevationGain;  // meters
    private Integer calories;
    private Double averageSpeed;    // km/h
    private Double maxSpeed;        // km/h
    private Integer averageHeartRate;
    private Integer maxHeartRate;
    private Integer averageCadence;
    private Integer averagePower;
    private LocalDateTime startTime;
    private String startLocation;
    private String endLocation;

    @Builder.Default
    private List<GpsPoint> gpsPoints = new ArrayList<>();
}
