package com.strava.backend.dto.gear;

import com.strava.backend.enums.GearType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Slim, nested gear summary exposed on ActivityResponse / FullActivityResponse.
 * Deliberately only carries id/name/type so the frontend can render
 * "Gear: Saucony Endorphin Pro 3" without bloating every activity payload
 * with full mileage totals / retirement status.
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
