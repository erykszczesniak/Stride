package com.strava.backend.service.importer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GpxParser Unit Tests")
class GpxParserTest {

    private GpxParser parser;

    @BeforeEach
    void setUp() {
        parser = new GpxParser();
    }

    @Test
    @DisplayName("parses a minimal GPX and captures per-point elevations")
    void parsesPerPointElevations() throws Exception {
        // Hand-rolled 3-point GPX with <ele> tags. Simpler than a file fixture and
        // isolated from whatever sample files live in main/resources.
        String gpx = """
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1" creator="test">
                  <trk>
                    <name>Test Hill</name>
                    <trkseg>
                      <trkpt lat="50.0" lon="19.0"><ele>100.0</ele></trkpt>
                      <trkpt lat="50.001" lon="19.001"><ele>110.5</ele></trkpt>
                      <trkpt lat="50.002" lon="19.002"><ele>120.25</ele></trkpt>
                    </trkseg>
                  </trk>
                </gpx>
                """;

        ParsedActivity parsed = parser.parse(new ByteArrayInputStream(gpx.getBytes(StandardCharsets.UTF_8)));

        assertThat(parsed).isNotNull();
        assertThat(parsed.getGpsPoints()).hasSize(3);
        assertThat(parsed.getElevations())
                .as("per-point elevations must be captured when <ele> tags are present")
                .isNotEmpty()
                .hasSize(3)
                .containsExactly(100.0, 110.5, 120.25);
    }

    @Test
    @DisplayName("returns empty elevations when GPX has no <ele> tags")
    void returnsEmptyElevationsWhenMissing() throws Exception {
        String gpx = """
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1" creator="test">
                  <trk>
                    <trkseg>
                      <trkpt lat="50.0" lon="19.0"/>
                      <trkpt lat="50.001" lon="19.001"/>
                    </trkseg>
                  </trk>
                </gpx>
                """;

        ParsedActivity parsed = parser.parse(new ByteArrayInputStream(gpx.getBytes(StandardCharsets.UTF_8)));

        assertThat(parsed.getGpsPoints()).hasSize(2);
        assertThat(parsed.getElevations())
                .as("no <ele> tags should yield an empty elevations list, not a list of nulls")
                .isEmpty();
    }
}
