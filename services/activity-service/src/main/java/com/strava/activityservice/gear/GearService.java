package com.strava.activityservice.gear;

import com.strava.activityservice.domain.ActivityType;
import com.strava.activityservice.domain.Gear;
import com.strava.activityservice.domain.GearRepository;
import com.strava.activityservice.domain.GearType;
import com.strava.activityservice.exception.ApiRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.strava.activityservice.constants.ErrorMessage.*;

/**
 * Business logic ported from main's GearServiceImpl. The only deviation forced
 * by the service split: gear rows carry a logical {@code userId} instead of a
 * {@code User} relation, so no user-service round-trip happens on create.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GearService {

    private final GearRepository gearRepository;

    @Transactional
    public Gear createGear(Long userId, GearRequest request) {
        Set<ActivityType> activityTypes = resolveActivityTypes(request.getActivityTypes(), request.getType());

        Gear gear = Gear.builder()
                .userId(userId)
                .name(request.getName().trim())
                .brand(request.getBrand())
                .model(request.getModel())
                .type(request.getType())
                .activityTypes(activityTypes)
                .distanceKmTotal(0.0)
                .activityCount(0)
                .retired(false)
                .notes(request.getNotes())
                .build();

        Gear saved = gearRepository.save(gear);
        log.info("Created gear id={} user={} type={}", saved.getId(), userId, saved.getType());
        return saved;
    }

    @Transactional
    public Gear updateGear(Long gearId, Long userId, GearRequest request) {
        Gear gear = getOwnedGear(gearId, userId);

        gear.setName(request.getName().trim());
        gear.setBrand(request.getBrand());
        gear.setModel(request.getModel());
        gear.setType(request.getType());
        gear.setActivityTypes(resolveActivityTypes(request.getActivityTypes(), request.getType()));
        gear.setNotes(request.getNotes());

        return gearRepository.save(gear);
    }

    @Transactional
    public void deleteGear(Long gearId, Long userId) {
        Gear gear = getOwnedGear(gearId, userId);
        gearRepository.delete(gear);
        log.info("Deleted gear id={} user={}", gearId, userId);
    }

    @Transactional
    public Gear retireGear(Long gearId, Long userId) {
        Gear gear = getOwnedGear(gearId, userId);
        if (gear.isRetired()) {
            throw new ApiRequestException(GEAR_ALREADY_RETIRED, HttpStatus.BAD_REQUEST);
        }
        gear.setRetired(true);
        gear.setRetiredAt(LocalDateTime.now());
        return gearRepository.save(gear);
    }

    @Transactional(readOnly = true)
    public Gear getGearById(Long gearId, Long userId) {
        return getOwnedGear(gearId, userId);
    }

    @Transactional(readOnly = true)
    public List<Gear> getUserGear(Long userId, boolean includeRetired) {
        if (includeRetired) {
            return gearRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return gearRepository.findByUserIdAndRetiredFalseOrderByCreatedAtDesc(userId);
    }

    /**
     * Bump the denormalized mileage + activity count on a gear. Called by
     * ActivityService when an activity with a tagged gear is saved.
     */
    @Transactional
    public void incrementGearMileage(Long gearId, double distanceKm) {
        if (gearId == null || distanceKm <= 0) return;
        gearRepository.findById(gearId).ifPresent(gear -> {
            gear.setDistanceKmTotal(gear.getDistanceKmTotal() + distanceKm);
            gear.setActivityCount(gear.getActivityCount() + 1);
            gearRepository.save(gear);
        });
    }

    /**
     * Inverse of {@link #incrementGearMileage}. Called when an activity is
     * deleted or re-tagged to a different gear. Never goes below zero.
     */
    @Transactional
    public void decrementGearMileage(Long gearId, double distanceKm) {
        if (gearId == null || distanceKm <= 0) return;
        gearRepository.findById(gearId).ifPresent(gear -> {
            // Clamp at zero so we don't end up with negative mileage if
            // bookkeeping ever drifts — easier to detect than to debug.
            double newTotal = Math.max(0.0, gear.getDistanceKmTotal() - distanceKm);
            int newCount = Math.max(0, gear.getActivityCount() - 1);
            gear.setDistanceKmTotal(newTotal);
            gear.setActivityCount(newCount);
            gearRepository.save(gear);
        });
    }

    private Gear getOwnedGear(Long gearId, Long userId) {
        Gear gear = gearRepository.findById(gearId)
                .orElseThrow(() -> new ApiRequestException(GEAR_NOT_FOUND, HttpStatus.NOT_FOUND));
        if (!gear.getUserId().equals(userId)) {
            throw new ApiRequestException(GEAR_ACCESS_DENIED, HttpStatus.FORBIDDEN);
        }
        return gear;
    }

    /**
     * If the caller omits activityTypes, pick a sensible default based on
     * gear type so the UX doesn't require a user to manually tick every
     * applicable sport. SHOES default to running-family sports, BIKE to
     * riding-family. OTHER gets an empty set — the user must be explicit.
     */
    private Set<ActivityType> resolveActivityTypes(Set<ActivityType> provided, GearType gearType) {
        if (provided != null && !provided.isEmpty()) {
            return EnumSet.copyOf(provided);
        }
        if (gearType == GearType.SHOES) {
            return EnumSet.of(ActivityType.RUN, ActivityType.VIRTUAL_RUN, ActivityType.WALK, ActivityType.HIKE);
        }
        if (gearType == GearType.BIKE) {
            return EnumSet.of(ActivityType.RIDE, ActivityType.VIRTUAL_RIDE);
        }
        return EnumSet.noneOf(ActivityType.class);
    }
}
