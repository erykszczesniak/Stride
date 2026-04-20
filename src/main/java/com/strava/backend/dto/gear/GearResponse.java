package com.strava.backend.dto.gear;

import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.GearType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

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
