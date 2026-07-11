package com.strava.backend.service.pipeline;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.User;
import com.strava.backend.enums.ActivityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ActivityStreamAnalyzer")
class ActivityStreamAnalyzerTest {

    private final ActivityStreamAnalyzer analyzer = new ActivityStreamAnalyzer();

    /**
     * Straight-north synthetic track: consecutive points are 0.0009° of
     * latitude apart, i.e. ~100.07 m of haversine distance per segment.
     */
    private static String syntheticPolyline(int points) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < points; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("[").append(52.0 + i * 0.0009).append(",21.0]");
        }
        return sb.append("]").toString();
    }

    /** First ten segments take 30 s each (fast km), the rest 36 s (slow km). */
    private static String syntheticTimestamps(int points) {
        StringBuilder sb = new StringBuilder("[");
        long t = 1_000_000_000L;
        for (int i = 0; i < points; i++) {
            if (i > 0) {
                t += i <= 10 ? 30 : 36;
                sb.append(",");
            }
            sb.append(t);
        }
        return sb.append("]").toString();
    }

    private static Activity activity(ActivityType type, double distanceKm, int durationSeconds,
                                     String polyline, String timestamps) {
        return Activity.builder()
                .id(42L)
                .title("test")
                .activityType(type)
                .distance(distanceKm)
                .duration(durationSeconds)
                .startTime(LocalDateTime.now())
                .routePolyline(polyline)
                .pointTimestamps(timestamps)
                .user(User.builder().id(1L).build())
                .build();
    }

    @Nested
    @DisplayName("computeSplits")
    class ComputeSplits {

        @Test
        @DisplayName("derives per-split durations from aligned GPS timestamps")
        void accurateSplitsFromTimestamps() {
            Activity activity = activity(ActivityType.RUN, 2.0, 660,
                    syntheticPolyline(21), syntheticTimestamps(21));

            List<ActivityStreamAnalyzer.SplitSegment> splits = analyzer.computeSplits(activity);

            assertThat(splits).hasSize(2);
            assertThat(splits.get(0).distanceMeters()).isEqualTo(1000);
            assertThat(splits.get(0).durationSeconds()).isEqualTo(300);
            assertThat(splits.get(0).accurate()).isTrue();
            assertThat(splits.get(1).distanceMeters()).isEqualTo(2000);
            assertThat(splits.get(1).durationSeconds()).isEqualTo(360);
            // The whole point of the accurate path: splits differ.
            assertThat(splits.get(0).durationSeconds()).isNotEqualTo(splits.get(1).durationSeconds());
        }

        @Test
        @DisplayName("closes a final partial split with the leftover distance")
        void partialSplit() {
            Activity activity = activity(ActivityType.RUN, 2.4, 804,
                    syntheticPolyline(25), syntheticTimestamps(25));

            List<ActivityStreamAnalyzer.SplitSegment> splits = analyzer.computeSplits(activity);

            assertThat(splits).hasSize(3);
            ActivityStreamAnalyzer.SplitSegment partial = splits.get(2);
            assertThat(partial.distanceMeters()).isBetween(2395, 2410);
            assertThat(partial.durationSeconds()).isEqualTo(4 * 36);
        }

        @Test
        @DisplayName("falls back to uniform splits without timestamps")
        void uniformFallback() {
            Activity activity = activity(ActivityType.RUN, 2.5, 750, null, null);

            List<ActivityStreamAnalyzer.SplitSegment> splits = analyzer.computeSplits(activity);

            assertThat(splits).hasSize(3);
            assertThat(splits.get(0).accurate()).isFalse();
            assertThat(splits.get(0).durationSeconds()).isEqualTo(splits.get(1).durationSeconds());
            assertThat(splits.get(2).distanceMeters()).isEqualTo(2500);
            assertThat(splits.get(2).durationSeconds()).isEqualTo(150);
        }

        @Test
        @DisplayName("falls back when timestamps are misaligned with the polyline")
        void misalignedTimestampsFallBack() {
            Activity activity = activity(ActivityType.RUN, 2.0, 660,
                    syntheticPolyline(21), syntheticTimestamps(20));

            List<ActivityStreamAnalyzer.SplitSegment> splits = analyzer.computeSplits(activity);

            assertThat(splits).isNotEmpty();
            assertThat(splits.get(0).accurate()).isFalse();
        }

        @Test
        @DisplayName("buckets are discipline-specific (1 km run, 10 km ride, 400 m swim)")
        void bucketSizes() {
            assertThat(ActivityStreamAnalyzer.splitBucketMeters(ActivityType.RUN)).isEqualTo(1000);
            assertThat(ActivityStreamAnalyzer.splitBucketMeters(ActivityType.RIDE)).isEqualTo(10_000);
            assertThat(ActivityStreamAnalyzer.splitBucketMeters(ActivityType.SWIM)).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("paceSeries")
    class PaceSeries {

        @Test
        @DisplayName("resamples real pace over equal-distance segments (sec/km for runs)")
        void accuratePace() {
            Activity activity = activity(ActivityType.RUN, 2.0, 660,
                    syntheticPolyline(21), syntheticTimestamps(21));

            List<Double> series = analyzer.paceSeries(activity);

            assertThat(series).hasSize(ActivityStreamAnalyzer.SERIES_SAMPLES);
            // Fast first half (~300 s/km) and slow second half (~360 s/km).
            assertThat(series.get(0)).isBetween(280.0, 320.0);
            assertThat(series.get(series.size() - 1)).isBetween(340.0, 380.0);
        }

        @Test
        @DisplayName("estimated ride series stays near the average speed in km/h")
        void estimatedBikeSeries() {
            Activity activity = activity(ActivityType.RIDE, 20.0, 3600, null, null);

            List<Double> series = analyzer.paceSeries(activity);

            assertThat(series).hasSize(ActivityStreamAnalyzer.SERIES_SAMPLES);
            assertThat(series).allSatisfy(v -> assertThat(v).isBetween(18.0, 22.0));
            // The deterministic ripple must keep the chart's min/max normalisation alive.
            assertThat(series.stream().distinct().count()).isGreaterThan(1);
        }
    }

    @Nested
    @DisplayName("route and elevation payloads")
    class RouteAndElevation {

        @Test
        @DisplayName("legacy JSON polylines are re-encoded so clients need one decoder")
        void legacyJsonPolylineIsReencoded() {
            Activity activity = activity(ActivityType.RUN, 2.0, 660, syntheticPolyline(21), null);

            String encoded = analyzer.encodedRoutePolyline(activity);

            assertThat(encoded).isNotNull().doesNotStartWith("[");
            var decoded = com.strava.backend.service.importer.GeoUtils.decodePolyline(encoded);
            assertThat(decoded).hasSize(21);
            assertThat(decoded.get(0)[0]).isCloseTo(52.0, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("already-encoded polylines pass through untouched")
        void encodedPolylinePassesThrough() {
            Activity activity = activity(ActivityType.RUN, 2.0, 660, "_p~iF~ps|U_ulLnnqC_mqNvxq`@", null);

            assertThat(analyzer.encodedRoutePolyline(activity)).isEqualTo("_p~iF~ps|U_ulLnnqC_mqNvxq`@");
        }

        @Test
        @DisplayName("single-point or missing routes yield null")
        void degenerateRoutesYieldNull() {
            assertThat(analyzer.encodedRoutePolyline(
                    activity(ActivityType.SWIM, 1.5, 2100, "[[52.2297,21.0017]]", null))).isNull();
            assertThat(analyzer.encodedRoutePolyline(
                    activity(ActivityType.RUN, 5.0, 1500, null, null))).isNull();
        }

        @Test
        @DisplayName("short elevation profiles pass through, long ones downsample to the cap")
        void elevationSeriesDownsamples() {
            Activity shortProfile = activity(ActivityType.RUN, 2.0, 660, null, null);
            shortProfile.setElevationProfile("[100.0,101.5,103.0,102.0,100.5]");
            assertThat(analyzer.elevationSeries(shortProfile)).containsExactly(100.0, 101.5, 103.0, 102.0, 100.5);

            StringBuilder longJson = new StringBuilder("[");
            for (int i = 0; i < 200; i++) {
                if (i > 0) longJson.append(",");
                longJson.append(100 + i);
            }
            longJson.append("]");
            Activity longProfile = activity(ActivityType.RUN, 2.0, 660, null, null);
            longProfile.setElevationProfile(longJson.toString());

            var series = analyzer.elevationSeries(longProfile);
            assertThat(series).hasSize(ActivityStreamAnalyzer.ELEVATION_MAX_SAMPLES);
            assertThat(series.get(0)).isEqualTo(100.0);
            assertThat(series.get(series.size() - 1)).isEqualTo(299.0);
        }

        @Test
        @DisplayName("missing elevation yields an empty series")
        void missingElevationYieldsEmpty() {
            assertThat(analyzer.elevationSeries(activity(ActivityType.RUN, 5.0, 1500, null, null))).isEmpty();
        }
    }

    @Nested
    @DisplayName("heart rate")
    class HeartRate {

        @Test
        @DisplayName("series drifts upward and never exceeds the recorded max")
        void hrSeries() {
            Activity activity = activity(ActivityType.RUN, 5.0, 1500, null, null);
            activity.setAverageHeartRate(150);
            activity.setMaxHeartRate(165);

            List<Double> series = analyzer.heartRateSeries(activity);

            assertThat(series).hasSize(ActivityStreamAnalyzer.SERIES_SAMPLES);
            assertThat(series).allSatisfy(v -> assertThat(v).isBetween(127.0, 165.0));
            assertThat(series.get(series.size() - 1)).isGreaterThan(series.get(0));
        }

        @Test
        @DisplayName("all zeros when the activity has no heart-rate data")
        void hrSeriesWithoutData() {
            Activity activity = activity(ActivityType.RUN, 5.0, 1500, null, null);

            assertThat(analyzer.heartRateSeries(activity))
                    .hasSize(ActivityStreamAnalyzer.SERIES_SAMPLES)
                    .allSatisfy(v -> assertThat(v).isZero());
            assertThat(analyzer.splitHeartRate(activity, 0, 5)).isZero();
        }

        @Test
        @DisplayName("per-split HR drifts upward within avg..max bounds")
        void splitHr() {
            Activity activity = activity(ActivityType.RUN, 5.0, 1500, null, null);
            activity.setAverageHeartRate(150);
            activity.setMaxHeartRate(165);

            int first = analyzer.splitHeartRate(activity, 0, 5);
            int last = analyzer.splitHeartRate(activity, 4, 5);

            assertThat(first).isBetween(128, 165);
            assertThat(last).isBetween(first, 165);
        }
    }
}
