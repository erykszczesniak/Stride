package com.strava.backend.mapper;

import com.strava.backend.domain.Gear;
import com.strava.backend.dto.gear.ActivityGearSummary;
import com.strava.backend.dto.gear.GearResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GearMapper {

    private final CommonMapper commonMapper;

    public GearResponse toResponse(Gear gear) {
        GearResponse response = commonMapper.convertToResponse(gear, GearResponse.class);
        // ModelMapper can mis-handle EnumSet (returns null for empty) — rebuild
        // the set explicitly from the entity so the frontend always sees a
        // real collection.
        response.setActivityTypes(
                gear.getActivityTypes() == null
                        ? EnumSet.noneOf(com.strava.backend.enums.ActivityType.class)
                        : EnumSet.copyOf(gear.getActivityTypes()));
        response.setDistanceFormatted(String.format("%.2f km", gear.getDistanceKmTotal()));
        return response;
    }

    public List<GearResponse> toResponseList(List<Gear> gearList) {
        return gearList.stream().map(this::toResponse).collect(Collectors.toList());
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
