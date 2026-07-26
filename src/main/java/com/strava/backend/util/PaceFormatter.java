package com.strava.backend.util;

import com.strava.backend.enums.ActivityType;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Formats pace/speed per sport:
 * - Run-like activities: min/km (metric) or min/mi (imperial)
 * - Swim: min/100m (metric) or min/100yd (imperial)
 * - Everything else (ride, etc.): km/h (metric) or mph (imperial)
 */
public final class PaceFormatter {

    private static final Set<ActivityType> PACE_TYPES =
            EnumSet.of(ActivityType.RUN, ActivityType.VIRTUAL_RUN, ActivityType.WALK, ActivityType.HIKE);

    private static final Set<ActivityType> SWIM_TYPES = EnumSet.of(ActivityType.SWIM);

    private static final double KM_TO_MI = 0.621371;
    private static final double HUNDRED_YD_PER_KM = 10.9361;

    private PaceFormatter() {
    }

    public static String format(ActivityType type, Double distanceKm, Integer durationSeconds, boolean imperial) {
        if (distanceKm == null || distanceKm <= 0 || durationSeconds == null || durationSeconds <= 0) {
            return "-";
        }

        if (SWIM_TYPES.contains(type)) {
            double unitsPerKm = imperial ? HUNDRED_YD_PER_KM : 10.0;
            double secondsPerUnit = durationSeconds / (distanceKm * unitsPerKm);
            return minSec(secondsPerUnit) + (imperial ? " /100yd" : " /100m");
        }

        if (PACE_TYPES.contains(type)) {
            double distance = imperial ? distanceKm * KM_TO_MI : distanceKm;
            double secondsPerUnit = durationSeconds / distance;
            return minSec(secondsPerUnit) + (imperial ? " min/mi" : " min/km");
        }

        double speedKmh = distanceKm / (durationSeconds / 3600.0);
        double speed = imperial ? speedKmh * KM_TO_MI : speedKmh;
        return String.format(Locale.US, "%.1f %s", speed, imperial ? "mph" : "km/h");
    }

    private static String minSec(double totalSeconds) {
        int minutes = (int) (totalSeconds / 60);
        int seconds = (int) Math.round(totalSeconds % 60);
        if (seconds == 60) {
            minutes++;
            seconds = 0;
        }
        return String.format("%d:%02d", minutes, seconds);
    }
}
