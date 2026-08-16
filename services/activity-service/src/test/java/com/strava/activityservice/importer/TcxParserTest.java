package com.strava.activityservice.importer;

import com.strava.activityservice.domain.ActivityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TcxParser Unit Tests")
class TcxParserTest {

    private TcxParser parser;

    @BeforeEach
    void setUp() {
        parser = new TcxParser();
    }

    private ByteArrayInputStream in(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("throws IllegalArgumentException when no Activity element is present")
    void throwsWithoutActivityElement() {
        String tcx = """
                <?xml version="1.0" encoding="UTF-8"?>
                <TrainingCenterDatabase></TrainingCenterDatabase>
                """;

        assertThatThrownBy(() -> parser.parse(in(tcx)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No Activity element");
    }

    @Test
    @DisplayName("aggregates lap totals and maps the Sport attribute to an ActivityType")
    void aggregatesLaps() throws Exception {
        String tcx = """
                <?xml version="1.0" encoding="UTF-8"?>
                <TrainingCenterDatabase>
                  <Activities>
                    <Activity Sport="Running">
                      <Id>2026-01-01T08:00:00Z</Id>
                      <Lap StartTime="2026-01-01T08:00:00Z">
                        <TotalTimeSeconds>600</TotalTimeSeconds>
                        <DistanceMeters>2000</DistanceMeters>
                        <Calories>120</Calories>
                        <AverageHeartRateBpm><Value>150</Value></AverageHeartRateBpm>
                        <MaximumHeartRateBpm><Value>165</Value></MaximumHeartRateBpm>
                      </Lap>
                      <Lap StartTime="2026-01-01T08:10:00Z">
                        <TotalTimeSeconds>660</TotalTimeSeconds>
                        <DistanceMeters>2100</DistanceMeters>
                        <Calories>130</Calories>
                        <AverageHeartRateBpm><Value>158</Value></AverageHeartRateBpm>
                        <MaximumHeartRateBpm><Value>172</Value></MaximumHeartRateBpm>
                      </Lap>
                    </Activity>
                  </Activities>
                </TrainingCenterDatabase>
                """;

        ParsedActivity parsed = parser.parse(in(tcx));

        assertThat(parsed.getActivityType()).isEqualTo(ActivityType.RUN);
        assertThat(parsed.getDuration()).isEqualTo(1260);
        // 4100m -> 4.1km
        assertThat(parsed.getDistance()).isEqualTo(4.1);
        assertThat(parsed.getCalories()).isEqualTo(250);
        assertThat(parsed.getMaxHeartRate()).isEqualTo(172);
        assertThat(parsed.getAverageHeartRate()).isEqualTo(154);
    }

    @Test
    @DisplayName("uses the first Notes line as the title and the full Notes as description")
    void usesNotesForTitleAndDescription() throws Exception {
        String tcx = """
                <?xml version="1.0" encoding="UTF-8"?>
                <TrainingCenterDatabase>
                  <Activities>
                    <Activity Sport="Biking">
                      <Id>2026-01-01T08:00:00Z</Id>
                      <Notes>5x1km intervals
                Felt strong today.</Notes>
                      <Lap StartTime="2026-01-01T08:00:00Z">
                        <TotalTimeSeconds>300</TotalTimeSeconds>
                        <DistanceMeters>1000</DistanceMeters>
                      </Lap>
                    </Activity>
                  </Activities>
                </TrainingCenterDatabase>
                """;

        ParsedActivity parsed = parser.parse(in(tcx));

        assertThat(parsed.getActivityType()).isEqualTo(ActivityType.RIDE);
        assertThat(parsed.getTitle()).isEqualTo("5x1km intervals");
        assertThat(parsed.getDescription()).contains("Felt strong today.");
    }

    @Test
    @DisplayName("falls back to 'Imported {sport}' when Notes are absent")
    void fallsBackToImportedSportTitle() throws Exception {
        String tcx = """
                <?xml version="1.0" encoding="UTF-8"?>
                <TrainingCenterDatabase>
                  <Activities>
                    <Activity Sport="Swimming">
                      <Id>2026-01-01T08:00:00Z</Id>
                      <Lap StartTime="2026-01-01T08:00:00Z">
                        <TotalTimeSeconds>1800</TotalTimeSeconds>
                        <DistanceMeters>1500</DistanceMeters>
                      </Lap>
                    </Activity>
                  </Activities>
                </TrainingCenterDatabase>
                """;

        ParsedActivity parsed = parser.parse(in(tcx));

        assertThat(parsed.getActivityType()).isEqualTo(ActivityType.SWIM);
        assertThat(parsed.getTitle()).isEqualTo("Imported Swimming");
        assertThat(parsed.getDescription()).isNull();
    }

    @Test
    @DisplayName("parses trackpoints with position, altitude, heart rate and time")
    void parsesTrackpoints() throws Exception {
        String tcx = """
                <?xml version="1.0" encoding="UTF-8"?>
                <TrainingCenterDatabase>
                  <Activities>
                    <Activity Sport="Running">
                      <Id>2026-01-01T08:00:00Z</Id>
                      <Lap StartTime="2026-01-01T08:00:00Z">
                        <TotalTimeSeconds>60</TotalTimeSeconds>
                        <DistanceMeters>0</DistanceMeters>
                        <Track>
                          <Trackpoint>
                            <Time>2026-01-01T08:00:00Z</Time>
                            <Position>
                              <LatitudeDegrees>52.2297</LatitudeDegrees>
                              <LongitudeDegrees>21.0122</LongitudeDegrees>
                            </Position>
                            <AltitudeMeters>100.0</AltitudeMeters>
                            <HeartRateBpm><Value>140</Value></HeartRateBpm>
                            <Cadence>85</Cadence>
                          </Trackpoint>
                          <Trackpoint>
                            <Time>2026-01-01T08:00:30Z</Time>
                            <Position>
                              <LatitudeDegrees>52.2300</LatitudeDegrees>
                              <LongitudeDegrees>21.0130</LongitudeDegrees>
                            </Position>
                            <AltitudeMeters>105.0</AltitudeMeters>
                            <HeartRateBpm><Value>150</Value></HeartRateBpm>
                            <Cadence>88</Cadence>
                          </Trackpoint>
                        </Track>
                      </Lap>
                    </Activity>
                  </Activities>
                </TrainingCenterDatabase>
                """;

        ParsedActivity parsed = parser.parse(in(tcx));

        assertThat(parsed.getGpsPoints()).hasSize(2);
        assertThat(parsed.getGpsPoints().get(0).getLatitude()).isEqualTo(52.2297);
        assertThat(parsed.getGpsPoints().get(0).getHeartRate()).isEqualTo(140);
        assertThat(parsed.getGpsPoints().get(1).getElevation()).isEqualTo(105.0);
        // Streams aligned 1:1 with the GPS points
        assertThat(parsed.getElevations()).containsExactly(100.0, 105.0);
        assertThat(parsed.getPointTimestampsEpochSec()).hasSize(2);
        assertThat(parsed.getPointTimestampsEpochSec().get(1) - parsed.getPointTimestampsEpochSec().get(0))
                .isEqualTo(30L);
        // Cadence average from trackpoints
        assertThat(parsed.getAverageCadence()).isEqualTo(86);
        // Start location comes from the first point. Mirrors main's
        // default-locale String.format, so build the expectation the same way.
        assertThat(parsed.getStartLocation())
                .isEqualTo(String.format("%.4f, %.4f", 52.2297, 21.0122));
    }

    @Test
    @DisplayName("supports only the tcx extension (case-insensitive)")
    void supportsOnlyTcx() {
        assertThat(parser.supports("tcx")).isTrue();
        assertThat(parser.supports("TCX")).isTrue();
        assertThat(parser.supports("gpx")).isFalse();
    }
}
