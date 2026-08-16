package com.strava.activityservice.gear;

import com.strava.activityservice.activity.ActivityGearSummary;
import com.strava.activityservice.domain.ActivityType;
import com.strava.activityservice.domain.Gear;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;

/**
 * Entity → DTO assembly, ported from main's GearMapper. Main routes through
 * ModelMapper (CommonMapper); this module maps explicitly like the rest of
 * Phase 13 — the JSON shape is identical.
 */
@Component
public class GearMapper {

    public GearResponse toResponse(Gear gear) {
        return GearResponse.builder()
                .id(gear.getId())
                .name(gear.getName())
                .brand(gear.getBrand())
                .model(gear.getModel())
                .type(gear.getType())
                // Rebuild the set explicitly from the entity so the frontend
                // always sees a real collection (mirrors main's ModelMapper
                // EnumSet workaround).
                .activityTypes(gear.getActivityTypes() == null || gear.getActivityTypes().isEmpty()
                        ? EnumSet.noneOf(ActivityType.class)
                        : EnumSet.copyOf(gear.getActivityTypes()))
                .distanceKmTotal(gear.getDistanceKmTotal())
                .activityCount(gear.getActivityCount())
                .retired(gear.isRetired())
                .retiredAt(gear.getRetiredAt())
                .notes(gear.getNotes())
                .createdAt(gear.getCreatedAt())
                .distanceFormatted(String.format("%.2f km", gear.getDistanceKmTotal()))
                .build();
    }

    public List<GearResponse> toResponseList(List<Gear> gearList) {
        return gearList.stream().map(this::toResponse).toList();
    }

    public ActivityGearSummary toSummary(Gear gear) {
        if (gear == null) return null;
        return ActivityGearSummary.builder()
                .id(gear.getId())
                .name(gear.getName())
                .type(gear.getType())
                .build();
    }
}
