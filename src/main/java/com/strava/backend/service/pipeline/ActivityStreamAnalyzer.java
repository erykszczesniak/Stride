package com.strava.backend.service.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.domain.Activity;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.service.importer.GeoUtils;
import com.strava.backend.service.importer.GpsPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Server-side counterpart of the web app's split/series math: derives per-split
 * times and evenly spaced pace/heart-rate series from an activity's stored GPS
 * streams (route polyline + per-point epoch timestamps).
 *
 * The accurate path requires timestamps aligned 1:1 with the decoded polyline —
 * the same precondition the React ActivityDetails page uses — and mirrors its
 * algorithm so both clients agree on the numbers. Without aligned timestamps it
 * degrades to totals-derived estimates (uniform splits, average-pace series
 * with a small deterministic ripple so charts stay renderable).
 *
 * Heart-rate data exists only as per-activity averages, so series and per-split
 * values are synthesized with a deterministic cardiac-drift model seeded by the
 * activity id: they are plausible, stable across calls, and bounded by the
 * recorded max — but not measurements.
 */
@Component
@Slf4j
public class ActivityStreamAnalyzer {

    public static final int SERIES_SAMPLES = 17;
    public static final int ELEVATION_MAX_SAMPLES = 60;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** A closed split: cumulative end distance, segment duration, accuracy flag. */
    public record SplitSegment(int distanceMeters, int durationSeconds, boolean accurate) {
    }

    /** Meters per split bucket, by discipline (runs 1 km, rides 10 km, swims 400 m). */
    public static int splitBucketMeters(ActivityType type) {
        return switch (type) {
            case RIDE, VIRTUAL_RIDE -> 10_000;
            case SWIM -> 400;
            default -> 1_000;
        };
    }

    public List<SplitSegment> computeSplits(Activity activity) {
        int bucket = splitBucketMeters(activity.getActivityType());
        Streams streams = parseStreams(activity);

        if (streams.aligned()) {
            List<SplitSegment> splits = accurateSplits(streams, bucket);
            if (!splits.isEmpty()) {
                return splits;
            }
            // Track shorter than one bucket — fall through to totals so the
            // client still gets a row for the whole effort.
        }
        return uniformSplits(activity, bucket);
    }

    private List<SplitSegment> accurateSplits(Streams s, int bucketMeters) {
        List<SplitSegment> splits = new ArrayList<>();
        int splitStartIdx = 0;
        double runningMeters = 0;
        int closedSplits = 0;

        for (int i = 1; i < s.coords.size(); i++) {
            runningMeters += segmentMeters(s.coords, i);

            // Close as many full splits as this segment spans (sparse tracks).
            while (runningMeters >= bucketMeters) {
                long duration = s.timestamps[i] - s.timestamps[splitStartIdx];
                closedSplits++;
                splits.add(new SplitSegment(
                        closedSplits * bucketMeters,
                        (int) Math.max(0, duration),
                        true));
                splitStartIdx = i;
                runningMeters -= bucketMeters;
            }
        }

        // Final partial split: leftover distance + tail duration.
        int lastIdx = s.coords.size() - 1;
        if (runningMeters > 10 && splitStartIdx < lastIdx) {
            long duration = s.timestamps[lastIdx] - s.timestamps[splitStartIdx];
            splits.add(new SplitSegment(
                    (int) Math.round(closedSplits * bucketMeters + runningMeters),
                    (int) Math.max(0, duration),
                    true));
        }
        return splits;
    }

    private List<SplitSegment> uniformSplits(Activity activity, int bucketMeters) {
        Double distanceKm = activity.getDistance();
        Integer duration = activity.getDuration();
        if (distanceKm == null || duration == null || distanceKm <= 0 || duration <= 0) {
            return List.of();
        }
        double totalMeters = distanceKm * 1000;
        int fullSplits = (int) Math.floor(totalMeters / bucketMeters);
        double secondsPerMeter = duration / totalMeters;

        List<SplitSegment> splits = new ArrayList<>();
        for (int i = 1; i <= fullSplits; i++) {
            splits.add(new SplitSegment(i * bucketMeters,
                    (int) Math.round(bucketMeters * secondsPerMeter), false));
        }
        double remainder = totalMeters - fullSplits * (double) bucketMeters;
        if (remainder > 10) {
            splits.add(new SplitSegment((int) Math.round(totalMeters),
                    (int) Math.round(remainder * secondsPerMeter), false));
        }
        return splits;
    }

    /**
     * Pace series resampled to {@value SERIES_SAMPLES} equal-distance segments.
     * Units per contract: sec/km (run/walk), km/h (ride), sec/100 m (swim).
     */
    public List<Double> paceSeries(Activity activity) {
        Streams s = parseStreams(activity);
        ActivityType type = activity.getActivityType();

        if (s.aligned()) {
            double[] cumMeters = new double[s.coords.size()];
            for (int i = 1; i < s.coords.size(); i++) {
                cumMeters[i] = cumMeters[i - 1] + segmentMeters(s.coords, i);
            }
            double total = cumMeters[cumMeters.length - 1];
            if (total > 0) {
                List<Double> series = new ArrayList<>(SERIES_SAMPLES);
                double segMeters = total / SERIES_SAMPLES;
                for (int k = 0; k < SERIES_SAMPLES; k++) {
                    double t0 = timeAtDistance(cumMeters, s.timestamps, segMeters * k);
                    double t1 = timeAtDistance(cumMeters, s.timestamps, segMeters * (k + 1));
                    double segSeconds = Math.max(1, t1 - t0);
                    series.add(round1(paceValue(type, segMeters, segSeconds)));
                }
                return series;
            }
        }

        // Estimated: average pace with a deterministic ripple so the chart has
        // shape and its min/max normalisation never divides by zero.
        Double distanceKm = activity.getDistance();
        Integer duration = activity.getDuration();
        if (distanceKm == null || duration == null || distanceKm <= 0 || duration <= 0) {
            return Collections.nCopies(SERIES_SAMPLES, 0.0);
        }
        double avg = paceValue(type, distanceKm * 1000, duration);
        double phase = seedPhase(activity.getId());
        List<Double> series = new ArrayList<>(SERIES_SAMPLES);
        for (int k = 0; k < SERIES_SAMPLES; k++) {
            double t = (double) k / (SERIES_SAMPLES - 1);
            double ripple = 1 + 0.05 * Math.sin(2 * Math.PI * 2 * t + phase)
                    + 0.03 * Math.sin(2 * Math.PI * 5 * t + phase * 1.7);
            // Ripple models effort (slower pace = higher seconds); speed is inverse.
            double value = type == ActivityType.RIDE || type == ActivityType.VIRTUAL_RIDE
                    ? avg / ripple
                    : avg * ripple;
            series.add(round1(value));
        }
        return series;
    }

    /**
     * Heart-rate series: cardiac-drift curve between ~94% and ~106% of the
     * activity average, bounded by the recorded max. All zeros when the
     * activity has no heart-rate data at all.
     */
    public List<Double> heartRateSeries(Activity activity) {
        Integer avg = activity.getAverageHeartRate();
        if (avg == null || avg <= 0) {
            return Collections.nCopies(SERIES_SAMPLES, 0.0);
        }
        int cap = activity.getMaxHeartRate() != null && activity.getMaxHeartRate() > avg
                ? activity.getMaxHeartRate()
                : (int) Math.round(avg * 1.10);
        double phase = seedPhase(activity.getId());
        List<Double> series = new ArrayList<>(SERIES_SAMPLES);
        for (int k = 0; k < SERIES_SAMPLES; k++) {
            double t = (double) k / (SERIES_SAMPLES - 1);
            double drift = avg * (0.94 + 0.10 * t);
            double wobble = avg * 0.03 * Math.sin(2 * Math.PI * 3 * t + phase);
            series.add((double) Math.round(clamp(drift + wobble, avg * 0.85, cap)));
        }
        return series;
    }

    /** Per-split average HR from the same drift model (0 when no HR recorded). */
    public int splitHeartRate(Activity activity, int splitIndex, int splitCount) {
        Integer avg = activity.getAverageHeartRate();
        if (avg == null || avg <= 0) {
            return 0;
        }
        int cap = activity.getMaxHeartRate() != null && activity.getMaxHeartRate() > avg
                ? activity.getMaxHeartRate()
                : (int) Math.round(avg * 1.10);
        double t = splitCount <= 1 ? 0.5 : (double) splitIndex / (splitCount - 1);
        double phase = seedPhase(activity.getId());
        double value = avg * (0.95 + 0.08 * t) + avg * 0.02 * Math.sin(2 * Math.PI * 2 * t + phase);
        return (int) Math.round(clamp(value, avg * 0.85, cap));
    }

    /** True when the activity has a polyline with 1:1 aligned timestamps. */
    public boolean hasAccurateStreams(Activity activity) {
        return parseStreams(activity).aligned();
    }

    /**
     * The activity's track as a Google Encoded Polyline, or null when there is
     * no usable multi-point route. Legacy rows store the route as a JSON
     * [[lat,lng]] string — those are re-encoded so native clients only ever
     * need one decoder.
     */
    public String encodedRoutePolyline(Activity activity) {
        List<double[]> coords = GeoUtils.decodePolyline(activity.getRoutePolyline());
        if (coords.size() < 2) {
            return null;
        }
        String raw = activity.getRoutePolyline().trim();
        if (!raw.startsWith("[")) {
            return raw;
        }
        List<GpsPoint> points = coords.stream()
                .map(pair -> GpsPoint.builder().latitude(pair[0]).longitude(pair[1]).build())
                .toList();
        return GeoUtils.encodePolyline(points);
    }

    /**
     * Altitude profile downsampled to at most {@value ELEVATION_MAX_SAMPLES}
     * evenly indexed points (charting payload). Empty when none is stored.
     */
    public List<Double> elevationSeries(Activity activity) {
        double[] profile = parseElevationProfile(activity.getElevationProfile());
        if (profile == null || profile.length == 0) {
            return List.of();
        }
        if (profile.length <= ELEVATION_MAX_SAMPLES) {
            List<Double> series = new ArrayList<>(profile.length);
            for (double value : profile) {
                series.add(round1(value));
            }
            return series;
        }
        List<Double> series = new ArrayList<>(ELEVATION_MAX_SAMPLES);
        for (int k = 0; k < ELEVATION_MAX_SAMPLES; k++) {
            int index = (int) Math.round((double) k * (profile.length - 1) / (ELEVATION_MAX_SAMPLES - 1));
            series.add(round1(profile[index]));
        }
        return series;
    }

    // --- internals -------------------------------------------------------

    private record Streams(List<double[]> coords, long[] timestamps) {
        boolean aligned() {
            return timestamps != null && timestamps.length > 1
                    && coords.size() == timestamps.length;
        }
    }

    private Streams parseStreams(Activity activity) {
        List<double[]> coords = GeoUtils.decodePolyline(activity.getRoutePolyline());
        return new Streams(coords, parsePointTimestamps(activity.getPointTimestamps()));
    }

    private long[] parsePointTimestamps(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            Long[] boxed = objectMapper.readValue(json, Long[].class);
            long[] result = new long[boxed.length];
            for (int i = 0; i < boxed.length; i++) {
                result[i] = boxed[i] != null ? boxed[i] : 0L;
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse point timestamps JSON: {}", e.getMessage());
            return null;
        }
    }

    private double[] parseElevationProfile(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            Double[] boxed = objectMapper.readValue(json, Double[].class);
            double[] result = new double[boxed.length];
            for (int i = 0; i < boxed.length; i++) {
                result[i] = boxed[i] != null ? boxed[i] : 0.0;
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse elevation profile JSON: {}", e.getMessage());
            return null;
        }
    }

    private static double segmentMeters(List<double[]> coords, int i) {
        double[] a = coords.get(i - 1);
        double[] b = coords.get(i);
        return GeoUtils.haversineMeters(a[0], a[1], b[0], b[1]);
    }

    /** Linear interpolation of the timestamp at a cumulative distance. */
    private static double timeAtDistance(double[] cumMeters, long[] timestamps, double target) {
        if (target <= 0) {
            return timestamps[0];
        }
        int n = cumMeters.length;
        if (target >= cumMeters[n - 1]) {
            return timestamps[n - 1];
        }
        int lo = 0;
        int hi = n - 1;
        while (lo + 1 < hi) {
            int mid = (lo + hi) >>> 1;
            if (cumMeters[mid] <= target) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        double span = cumMeters[hi] - cumMeters[lo];
        double frac = span <= 0 ? 0 : (target - cumMeters[lo]) / span;
        return timestamps[lo] + frac * (timestamps[hi] - timestamps[lo]);
    }

    private static double paceValue(ActivityType type, double meters, double seconds) {
        return switch (type) {
            case RIDE, VIRTUAL_RIDE -> (meters / 1000.0) / (seconds / 3600.0); // km/h
            case SWIM -> seconds / (meters / 100.0);                            // sec/100m
            default -> seconds / (meters / 1000.0);                             // sec/km
        };
    }

    private static double seedPhase(Long id) {
        long seed = id == null ? 0 : id;
        return (seed * 0.6180339887498949 % 1.0) * 2 * Math.PI;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
