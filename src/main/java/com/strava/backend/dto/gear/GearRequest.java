package com.strava.backend.dto.gear;

import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.GearType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.EnumSet;
import java.util.Set;

@Data
public class GearRequest {

    @NotBlank(message = "Gear name cannot be empty")
    private String name;

    private String brand;

    private String model;

    @NotNull(message = "Gear type is required")
    private GearType type;

    /**
     * Activity types this gear can be tagged with. Empty or null is allowed —
     * service layer defaults to a sensible set based on the gear type
     * (SHOES → RUN/TRAIL_RUN/VIRTUAL_RUN, BIKE → RIDE/VIRTUAL_RIDE).
     */
    private Set<ActivityType> activityTypes = EnumSet.noneOf(ActivityType.class);

    private String notes;
}
