package com.strava.backend.service.pipeline;

import com.strava.backend.domain.Activity;
import com.strava.backend.enums.ActivityType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classic Coggan/Banister training-load model computed from activity history.
 *
 * Per-activity load is heart-rate TSS: {@code hours * IF^2 * 100} with
 * {@code IF = avgHR / thresholdHR} (threshold fixed at 170 bpm — the athlete
 * profile has no lactate-threshold field). Activities without heart rate fall
 * back to a per-discipline typical intensity factor.
 *
 * CTL (fitness) and ATL (fatigue) are exponentially weighted moving averages
 * of daily TSS with 42- and 7-day time constants; TSB (form) = CTL − ATL.
 */
@Component
public class TrainingLoadCalculator {

    public static final double THRESHOLD_HR = 170.0;
    private static final double CTL_DAYS = 42.0;
    private static final double ATL_DAYS = 7.0;
    /** Warm-up window so the EMAs are settled before the first reported point. */
    private static final int WARMUP_DAYS = 84;

    public record DayLoad(LocalDate date, double ctl, double atl) {
        public double tsb() {
            return ctl - atl;
        }
    }

    public double tss(Activity activity) {
        Integer duration = activity.getDuration();
        if (duration == null || duration <= 0) {
            return 0;
        }
        double hours = duration / 3600.0;
        double intensity = intensityFactor(activity);
        return hours * intensity * intensity * 100.0;
    }

    private double intensityFactor(Activity activity) {
        Integer avgHr = activity.getAverageHeartRate();
        if (avgHr != null && avgHr > 0) {
            return clamp(avgHr / THRESHOLD_HR, 0.55, 1.10);
        }
        return switch (activity.getActivityType()) {
            case RUN, VIRTUAL_RUN -> 0.78;
            case SWIM -> 0.74;
            case RIDE, VIRTUAL_RIDE -> 0.70;
            default -> 0.65;
        };
    }

    public Map<LocalDate, Double> dailyTss(List<Activity> activities) {
        Map<LocalDate, Double> daily = new HashMap<>();
        for (Activity activity : activities) {
            if (activity.getStartTime() == null) {
                continue;
            }
            daily.merge(activity.getStartTime().toLocalDate(), tss(activity), Double::sum);
        }
        return daily;
    }

    /**
     * Daily CTL/ATL between {@code from} and {@code to} (inclusive). The EMAs
     * start from zero at the earlier of the first training day and a warm-up
     * window before {@code from}, so reported values are already settled.
     */
    public List<DayLoad> loadSeries(Map<LocalDate, Double> dailyTss, LocalDate from, LocalDate to) {
        LocalDate warmupStart = from.minusDays(WARMUP_DAYS);
        LocalDate start = dailyTss.keySet().stream()
                .min(LocalDate::compareTo)
                .filter(first -> first.isBefore(warmupStart))
                .orElse(warmupStart);

        List<DayLoad> series = new ArrayList<>();
        double ctl = 0;
        double atl = 0;
        for (LocalDate day = start; !day.isAfter(to); day = day.plusDays(1)) {
            double tss = dailyTss.getOrDefault(day, 0.0);
            ctl += (tss - ctl) / CTL_DAYS;
            atl += (tss - atl) / ATL_DAYS;
            if (!day.isBefore(from)) {
                series.add(new DayLoad(day, ctl, atl));
            }
        }
        return series;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
