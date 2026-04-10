package com.strava.backend.service.importer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

@DisplayName("GeoUtils Unit Tests")
class GeoUtilsTest {

    private static final double COORD_TOLERANCE = 1e-5;

    @Nested
    @DisplayName("decodePolyline Tests")
    class DecodePolylineTests {

        @Test
        @DisplayName("Should return empty list when input is null")
        void shouldReturnEmptyListWhenNull() {
            List<double[]> result = GeoUtils.decodePolyline(null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when input is blank")
        void shouldReturnEmptyListWhenBlank() {
            assertThat(GeoUtils.decodePolyline("")).isEmpty();
            assertThat(GeoUtils.decodePolyline("   ")).isEmpty();
        }

        @Test
        @DisplayName("Should parse legacy JSON polyline format")
        void shouldParseLegacyJsonFormat() {
            String json = "[[52.2297,21.0122],[52.2250,21.0155],[52.2200,21.0200]]";

            List<double[]> result = GeoUtils.decodePolyline(json);

            assertThat(result).hasSize(3);
            assertThat(result.get(0)[0]).isCloseTo(52.2297, offset(COORD_TOLERANCE));
            assertThat(result.get(0)[1]).isCloseTo(21.0122, offset(COORD_TOLERANCE));
            assertThat(result.get(1)[0]).isCloseTo(52.2250, offset(COORD_TOLERANCE));
            assertThat(result.get(1)[1]).isCloseTo(21.0155, offset(COORD_TOLERANCE));
            assertThat(result.get(2)[0]).isCloseTo(52.2200, offset(COORD_TOLERANCE));
            assertThat(result.get(2)[1]).isCloseTo(21.0200, offset(COORD_TOLERANCE));
        }

        @Test
        @DisplayName("Should parse Google Encoded Polyline format")
        void shouldParseGoogleEncodedFormat() {
            // Canonical example from Google's polyline algorithm docs
            String encoded = "_p~iF~ps|U_ulLnnqC_mqNvxq`@";

            List<double[]> result = GeoUtils.decodePolyline(encoded);

            assertThat(result).hasSize(3);
            assertThat(result.get(0)[0]).isCloseTo(38.5, offset(COORD_TOLERANCE));
            assertThat(result.get(0)[1]).isCloseTo(-120.2, offset(COORD_TOLERANCE));
            assertThat(result.get(1)[0]).isCloseTo(40.7, offset(COORD_TOLERANCE));
            assertThat(result.get(1)[1]).isCloseTo(-120.95, offset(COORD_TOLERANCE));
            assertThat(result.get(2)[0]).isCloseTo(43.252, offset(COORD_TOLERANCE));
            assertThat(result.get(2)[1]).isCloseTo(-126.453, offset(COORD_TOLERANCE));
        }

        @Test
        @DisplayName("Should return empty list for malformed JSON")
        void shouldReturnEmptyListForMalformedJson() {
            String brokenJson = "[[52.2297,21.0122],[not valid json";

            List<double[]> result = GeoUtils.decodePolyline(brokenJson);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return partial result for truncated encoded polyline without throwing")
        void shouldReturnPartialResultForTruncatedEncoded() {
            // Take a valid polyline and chop it mid-token so the inner do/while runs out of input
            String encoded = "_p~iF~ps|U_ulLnnqC_mqNvxq`@";
            String truncated = encoded.substring(0, encoded.length() - 2);

            List<double[]> result = GeoUtils.decodePolyline(truncated);

            // Should not throw IndexOutOfBoundsException and should return the points that did decode.
            assertThat(result).isNotNull();
            // The first complete point should have been decoded before truncation cut it off
            assertThat(result.size()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Should round-trip points through encodePolyline then decodePolyline")
        void shouldRoundTripPoints() {
            List<GpsPoint> original = List.of(
                    GpsPoint.builder().latitude(52.2297).longitude(21.0122).build(),
                    GpsPoint.builder().latitude(52.2350).longitude(21.0200).build(),
                    GpsPoint.builder().latitude(52.2400).longitude(21.0300).build(),
                    GpsPoint.builder().latitude(52.2500).longitude(21.0450).build()
            );

            String encoded = GeoUtils.encodePolyline(original);
            List<double[]> decoded = GeoUtils.decodePolyline(encoded);

            assertThat(decoded).hasSize(original.size());
            for (int i = 0; i < original.size(); i++) {
                assertThat(decoded.get(i)[0]).isCloseTo(original.get(i).getLatitude(), offset(COORD_TOLERANCE));
                assertThat(decoded.get(i)[1]).isCloseTo(original.get(i).getLongitude(), offset(COORD_TOLERANCE));
            }
        }
    }

    @Nested
    @DisplayName("haversineMeters Tests")
    class HaversineMetersTests {

        @Test
        @DisplayName("Should return approximately zero for identical points")
        void shouldReturnZeroForIdenticalPoints() {
            double distance = GeoUtils.haversineMeters(52.2297, 21.0122, 52.2297, 21.0122);

            assertThat(distance).isCloseTo(0.0, offset(0.0001));
        }

        @Test
        @DisplayName("Should return approximately 111 km for one degree of latitude")
        void shouldReturnAbout111KmForOneDegreeLatitude() {
            double distance = GeoUtils.haversineMeters(0.0, 0.0, 1.0, 0.0);

            // One degree of latitude is ~111 km anywhere on Earth
            assertThat(distance).isCloseTo(111_195.0, offset(500.0));
        }

        @Test
        @DisplayName("Should be symmetric: distance(A,B) == distance(B,A)")
        void shouldBeSymmetric() {
            double ab = GeoUtils.haversineMeters(52.2297, 21.0122, 50.0647, 19.9450);
            double ba = GeoUtils.haversineMeters(50.0647, 19.9450, 52.2297, 21.0122);

            assertThat(ab).isCloseTo(ba, offset(0.0001));
        }
    }

    @Nested
    @DisplayName("totalDistanceKm Tests")
    class TotalDistanceKmTests {

        @Test
        @DisplayName("Should return zero for empty list")
        void shouldReturnZeroForEmptyList() {
            double result = GeoUtils.totalDistanceKm(Collections.emptyList());

            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return zero for single point")
        void shouldReturnZeroForSinglePoint() {
            List<GpsPoint> points = List.of(
                    GpsPoint.builder().latitude(52.2297).longitude(21.0122).build()
            );

            double result = GeoUtils.totalDistanceKm(points);

            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return distance matching expected within tolerance")
        void shouldReturnExpectedDistance() {
            // (0,0) -> (1,0) -> (1,1): lat 1 deg (~111km) + lon 1 deg at lat=1 (~111km)
            List<GpsPoint> points = List.of(
                    GpsPoint.builder().latitude(0.0).longitude(0.0).build(),
                    GpsPoint.builder().latitude(1.0).longitude(0.0).build(),
                    GpsPoint.builder().latitude(1.0).longitude(1.0).build()
            );

            double result = GeoUtils.totalDistanceKm(points);

            // About 111 + 111 = 222 km, allow +/-1 km for haversine rounding
            assertThat(result).isCloseTo(222.3, offset(1.0));
        }
    }

    @Nested
    @DisplayName("totalElevationGain Tests")
    class TotalElevationGainTests {

        @Test
        @DisplayName("Should only count upward differences")
        void shouldOnlyCountUpwardDifferences() {
            List<GpsPoint> points = List.of(
                    GpsPoint.builder().latitude(0).longitude(0).elevation(100.0).build(),
                    GpsPoint.builder().latitude(0).longitude(0).elevation(150.0).build(), // +50
                    GpsPoint.builder().latitude(0).longitude(0).elevation(130.0).build(), // -20, ignored
                    GpsPoint.builder().latitude(0).longitude(0).elevation(170.0).build(), // +40
                    GpsPoint.builder().latitude(0).longitude(0).elevation(170.0).build()  // +0
            );

            int gain = GeoUtils.totalElevationGain(points);

            assertThat(gain).isEqualTo(90);
        }

        @Test
        @DisplayName("Should ignore points with null elevation")
        void shouldIgnoreNullElevation() {
            List<GpsPoint> points = List.of(
                    GpsPoint.builder().latitude(0).longitude(0).elevation(100.0).build(),
                    GpsPoint.builder().latitude(0).longitude(0).elevation(null).build(),
                    GpsPoint.builder().latitude(0).longitude(0).elevation(200.0).build(),
                    GpsPoint.builder().latitude(0).longitude(0).elevation(250.0).build() // +50
            );

            int gain = GeoUtils.totalElevationGain(points);

            // Only the 200 -> 250 segment counts (null pairs are skipped)
            assertThat(gain).isEqualTo(50);
        }

        @Test
        @DisplayName("Should return zero for empty list")
        void shouldReturnZeroForEmptyList() {
            assertThat(GeoUtils.totalElevationGain(Collections.emptyList())).isZero();
        }
    }

    @Nested
    @DisplayName("maxSpeedKmh Tests")
    class MaxSpeedKmhTests {

        @Test
        @DisplayName("Should filter out GPS noise above 200 km/h")
        void shouldFilterGpsNoiseAbove200Kmh() {
            LocalDateTime t0 = LocalDateTime.of(2026, 1, 1, 10, 0, 0);

            List<GpsPoint> points = List.of(
                    // Normal run pace: ~100m in 30s = 12 km/h
                    GpsPoint.builder().latitude(0.0).longitude(0.0).time(t0).build(),
                    GpsPoint.builder().latitude(0.0).longitude(0.001).time(t0.plusSeconds(30)).build(),
                    // GPS glitch: 1 degree of latitude (~111km) in 1 second = ~400,000 km/h (noise)
                    GpsPoint.builder().latitude(1.0).longitude(0.001).time(t0.plusSeconds(31)).build(),
                    // Back to normal: small hop shortly after
                    GpsPoint.builder().latitude(1.0).longitude(0.002).time(t0.plusSeconds(61)).build()
            );

            double maxSpeed = GeoUtils.maxSpeedKmh(points);

            // The giant glitch segment should be filtered out, leaving the normal segments (~13 km/h)
            assertThat(maxSpeed).isLessThan(200.0);
            assertThat(maxSpeed).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("Should return zero when points list is empty")
        void shouldReturnZeroForEmptyList() {
            assertThat(GeoUtils.maxSpeedKmh(Collections.emptyList())).isZero();
        }

        @Test
        @DisplayName("Should return zero when no timestamps are present")
        void shouldReturnZeroWhenNoTimestamps() {
            List<GpsPoint> points = List.of(
                    GpsPoint.builder().latitude(0.0).longitude(0.0).build(),
                    GpsPoint.builder().latitude(0.0).longitude(1.0).build()
            );

            assertThat(GeoUtils.maxSpeedKmh(points)).isZero();
        }
    }
}
