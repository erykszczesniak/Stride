package com.strava.activityservice.gear;

import com.strava.activityservice.domain.ActivityType;
import com.strava.activityservice.domain.GearType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/** Ported 1:1 from main — field names and JSON shape are the public contract. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GearResponse {
    private Long id;
    private String name;
    private String brand;
    private String model;
    private GearType type;
    private Set<ActivityType> activityTypes;
    private double distanceKmTotal;
    private int activityCount;
    private boolean retired;
    private LocalDateTime retiredAt;
    private String notes;
    private LocalDateTime createdAt;
    /** Pre-formatted distance, e.g. "542.33 km". */
    private String distanceFormatted;
}
