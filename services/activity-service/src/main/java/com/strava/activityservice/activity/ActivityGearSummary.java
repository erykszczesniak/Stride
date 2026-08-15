package com.strava.activityservice.activity;

import com.strava.activityservice.domain.GearType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Slim, nested gear summary exposed on ActivityResponse / FullActivityResponse
 * (ported 1:1 from main). Stays null until gear tracking lands in a later
 * phase, but the field shape is already part of the public contract.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityGearSummary {
    private Long id;
    private String name;
    private GearType type;
}
