package com.strava.backend.service;

import com.strava.backend.domain.Gear;
import com.strava.backend.dto.gear.GearRequest;

import java.util.List;

public interface GearService {

    Gear createGear(Long userId, GearRequest request);

    Gear updateGear(Long gearId, Long userId, GearRequest request);

    void deleteGear(Long gearId, Long userId);

    Gear retireGear(Long gearId, Long userId);

    Gear getGearById(Long gearId, Long userId);

    List<Gear> getUserGear(Long userId, boolean includeRetired);

    /**
     * Bump the denormalized mileage + activity count on a gear. Called by
     * ActivityService when an activity with a tagged gear is saved.
     */
    void incrementGearMileage(Long gearId, double distanceKm);

    /**
     * Inverse of {@link #incrementGearMileage}. Called when an activity is
     * deleted or re-tagged to a different gear. Never goes below zero.
     */
    void decrementGearMileage(Long gearId, double distanceKm);
}
