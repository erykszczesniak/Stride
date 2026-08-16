package com.strava.activityservice.importer;

import com.strava.activityservice.domain.ActivityType;
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

    // Per-point elevation in meters. Parallels gpsPoints 1:1 so downstream code
    // can align the elevation series with the encoded polyline. Entries may be
    // null when a given trackpoint had no altitude; the whole list is empty
    // when the source file carried no elevation data at all.
    @Builder.Default
    private List<Double> elevations = new ArrayList<>();

    // Per-point epoch-seconds timestamps. Parallels gpsPoints 1:1 when present so
    // the frontend can compute real per-km splits from the delta between
    // consecutive points. Empty when ANY source trackpoint lacked a timestamp —
    // we treat partial coverage as absent because interpolating gaps would be
    // more misleading than falling back to uniform-pace splits.
    @Builder.Default
    private List<Long> pointTimestampsEpochSec = new ArrayList<>();
}
