package com.strava.webbff.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaceFormatter Unit Tests")
class PaceFormatterTest {

    @Nested
    @DisplayName("Run-like activities")
    class RunPaceTests {

        @Test
        @DisplayName("Should format run pace as min/km in metric")
        void shouldFormatRunPaceMetric() {
            // 10 km in 50 min -> 5:00 min/km
            assertThat(PaceFormatter.format(ActivityType.RUN, 10.0, 3000, false))
                    .isEqualTo("5:00 min/km");
        }

        @Test
        @DisplayName("Should format run pace as min/mi in imperial")
        void shouldFormatRunPaceImperial() {
            // 10 km = 6.21371 mi in 3000 s -> 482.8 s/mi -> 8:03 min/mi
            assertThat(PaceFormatter.format(ActivityType.RUN, 10.0, 3000, true))
                    .isEqualTo("8:03 min/mi");
        }

        @Test
        @DisplayName("Should treat walk and hike as pace activities")
        void shouldFormatWalkAndHikeAsPace() {
            assertThat(PaceFormatter.format(ActivityType.WALK, 5.0, 3000, false))
                    .isEqualTo("10:00 min/km");
            assertThat(PaceFormatter.format(ActivityType.HIKE, 5.0, 3000, false))
                    .isEqualTo("10:00 min/km");
            assertThat(PaceFormatter.format(ActivityType.VIRTUAL_RUN, 10.0, 3000, false))
                    .isEqualTo("5:00 min/km");
        }

        @Test
        @DisplayName("Should carry seconds rounding into minutes")
        void shouldCarrySecondsRounding() {
            // 1 km in 299.6 s would round to 5:60 without a carry -> must be 5:00
            assertThat(PaceFormatter.format(ActivityType.RUN, 10.0, 2996, false))
                    .isEqualTo("5:00 min/km");
        }
    }

    @Nested
    @DisplayName("Swim activities")
    class SwimPaceTests {

        @Test
        @DisplayName("Should format swim pace as min/100m in metric")
        void shouldFormatSwimPaceMetric() {
            // 1 km in 1500 s -> 150 s per 100m -> 2:30 /100m
            assertThat(PaceFormatter.format(ActivityType.SWIM, 1.0, 1500, false))
                    .isEqualTo("2:30 /100m");
        }

        @Test
        @DisplayName("Should format swim pace as min/100yd in imperial")
        void shouldFormatSwimPaceImperial() {
            // 1 km = 10.9361 x 100yd in 1500 s -> 137.2 s per 100yd -> 2:17 /100yd
            assertThat(PaceFormatter.format(ActivityType.SWIM, 1.0, 1500, true))
                    .isEqualTo("2:17 /100yd");
        }
    }

    @Nested
    @DisplayName("Speed activities")
    class SpeedTests {

        @Test
        @DisplayName("Should format ride as km/h in metric")
        void shouldFormatRideMetric() {
            // 35 km in 5400 s (1.5 h) -> 23.3 km/h
            assertThat(PaceFormatter.format(ActivityType.RIDE, 35.0, 5400, false))
                    .isEqualTo("23.3 km/h");
        }

        @Test
        @DisplayName("Should format ride as mph in imperial")
        void shouldFormatRideImperial() {
            // 23.333 km/h * 0.621371 -> 14.5 mph
            assertThat(PaceFormatter.format(ActivityType.RIDE, 35.0, 5400, true))
                    .isEqualTo("14.5 mph");
        }

        @Test
        @DisplayName("Should format other non-pace activities as speed")
        void shouldFormatOtherTypesAsSpeed() {
            assertThat(PaceFormatter.format(ActivityType.KAYAKING, 10.0, 3600, false))
                    .isEqualTo("10.0 km/h");
            assertThat(PaceFormatter.format(ActivityType.WORKOUT, 10.0, 3600, false))
                    .isEqualTo("10.0 km/h");
        }
    }

    @Nested
    @DisplayName("Invalid input")
    class InvalidInputTests {

        @Test
        @DisplayName("Should return dash for missing or zero distance/duration")
        void shouldReturnDashForInvalidInput() {
            assertThat(PaceFormatter.format(ActivityType.RUN, null, 3000, false)).isEqualTo("-");
            assertThat(PaceFormatter.format(ActivityType.RUN, 0.0, 3000, false)).isEqualTo("-");
            assertThat(PaceFormatter.format(ActivityType.RUN, 10.0, null, false)).isEqualTo("-");
            assertThat(PaceFormatter.format(ActivityType.RUN, 10.0, 0, false)).isEqualTo("-");
        }
    }
}
