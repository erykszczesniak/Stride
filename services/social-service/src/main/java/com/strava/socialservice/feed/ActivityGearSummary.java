package com.strava.socialservice.feed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Slim, nested gear summary on ActivityResponse (ported 1:1 from main).
 * Always null on feed rows served by social-service — the internal batch API
 * deliberately doesn't expose gear — but the field shape is part of the
 * public contract, so it must exist and serialize as null.
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
