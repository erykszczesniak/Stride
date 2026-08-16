package com.strava.activityservice.importactivity;

import com.strava.activityservice.activity.ActivityRequest;
import com.strava.activityservice.activity.ActivityService;
import com.strava.activityservice.domain.Activity;
import com.strava.activityservice.domain.ActivityType;
import com.strava.activityservice.exception.ApiRequestException;
import com.strava.activityservice.importer.ActivityFileParser;
import com.strava.activityservice.importer.GpsPoint;
import com.strava.activityservice.importer.ParsedActivity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

import static com.strava.activityservice.constants.ErrorMessage.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Ported from main's ImportServiceImplTest, adapted to this module's deliberate
 * deviation: imports persist through ActivityService.createActivity (the same
 * pipeline as POST /api/v1/activities) rather than saving directly through the
 * repository — so the assertions capture the ActivityRequest instead of the
 * entity.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ImportService Unit Tests")
class ImportServiceTest {

    @Mock
    private ActivityFileParser gpxParser;

    @Mock
    private ActivityFileParser fitParser;

    @Mock
    private ActivityService activityService;

    private ImportService importService;

    @BeforeEach
    void setUp() {
        importService = new ImportService(List.of(gpxParser, fitParser), activityService);
    }

    @Nested
    @DisplayName("importActivity Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw ApiRequestException when file is null")
        void shouldThrowWhenFileIsNull() {
            assertThatThrownBy(() -> importService.importActivity(1L, null, null, null, false))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(IMPORT_FILE_EMPTY)
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should throw ApiRequestException when file is empty")
        void shouldThrowWhenFileIsEmpty() {
            MockMultipartFile file = new MockMultipartFile("file", "run.gpx", "application/xml", new byte[0]);

            assertThatThrownBy(() -> importService.importActivity(1L, file, null, null, false))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(IMPORT_FILE_EMPTY);
        }

        @Test
        @DisplayName("Should throw ApiRequestException for files larger than 10MB")
        void shouldThrowWhenFileTooLarge() {
            byte[] bigBytes = new byte[11 * 1024 * 1024];
            MockMultipartFile file = new MockMultipartFile("file", "run.gpx", "application/xml", bigBytes);

            assertThatThrownBy(() -> importService.importActivity(1L, file, null, null, false))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(IMPORT_FILE_TOO_LARGE);
        }

        @Test
        @DisplayName("Should throw ApiRequestException for unsupported file extensions")
        void shouldThrowForUnsupportedExtension() {
            MockMultipartFile file = new MockMultipartFile("file", "run.pdf", "application/pdf", "junk".getBytes());

            assertThatThrownBy(() -> importService.importActivity(1L, file, null, null, false))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(UNSUPPORTED_FILE_FORMAT);
        }

        @Test
        @DisplayName("Should throw ApiRequestException when filename has no extension")
        void shouldThrowWhenFilenameHasNoExtension() {
            MockMultipartFile file = new MockMultipartFile("file", "noextension", "application/octet-stream", "junk".getBytes());

            assertThatThrownBy(() -> importService.importActivity(1L, file, null, null, false))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(UNSUPPORTED_FILE_FORMAT);
        }
    }

    @Nested
    @DisplayName("importActivity Parser Selection + Pipeline Tests")
    class ParserSelectionTests {

        @Test
        @DisplayName("Should pick the GPX parser for a .gpx file and persist through the create pipeline")
        void shouldPickGpxParser() throws Exception {
            byte[] content = "<gpx></gpx>".getBytes();
            MockMultipartFile file = new MockMultipartFile("file", "run.gpx", "application/xml", content);

            ParsedActivity parsed = ParsedActivity.builder()
                    .title("Morning Run")
                    .activityType(ActivityType.RUN)
                    .distance(5.0)
                    .duration(1500)
                    .startTime(LocalDateTime.of(2026, 1, 1, 8, 0))
                    .gpsPoints(List.of())
                    .build();

            when(gpxParser.supports("gpx")).thenReturn(true);
            when(gpxParser.parse(any(InputStream.class))).thenReturn(parsed);

            ArgumentCaptor<ActivityRequest> reqCaptor = ArgumentCaptor.forClass(ActivityRequest.class);
            Activity created = Activity.builder().id(99L).title("Morning Run")
                    .activityType(ActivityType.RUN).distance(5.0).duration(1500).build();
            when(activityService.createActivity(eq(1L), reqCaptor.capture())).thenReturn(created);

            Activity result = importService.importActivity(1L, file, null, null, false);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(99L);
            ActivityRequest req = reqCaptor.getValue();
            assertThat(req.getTitle()).isEqualTo("Morning Run");
            assertThat(req.getActivityType()).isEqualTo(ActivityType.RUN);
            assertThat(req.getDistance()).isEqualTo(5.0);
            assertThat(req.getDuration()).isEqualTo(1500);
            verify(gpxParser).parse(any(InputStream.class));
            verify(fitParser, never()).parse(any());
            verify(activityService).createActivity(eq(1L), any(ActivityRequest.class));
        }

        @Test
        @DisplayName("Should throw ApiRequestException when no parser supports the extension")
        void shouldThrowWhenNoParserSupportsExtension() {
            // A .tcx file passes the ALLOWED_EXTENSIONS check but no mocked parser supports it
            MockMultipartFile file = new MockMultipartFile("file", "run.tcx", "application/xml", "<tcx></tcx>".getBytes());
            when(gpxParser.supports("tcx")).thenReturn(false);
            when(fitParser.supports("tcx")).thenReturn(false);

            assertThatThrownBy(() -> importService.importActivity(1L, file, null, null, false))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(UNSUPPORTED_FILE_FORMAT);
        }

        @Test
        @DisplayName("Should encode GPS points into the request's route polyline")
        void shouldEncodePolylineWhenGpsPointsPresent() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "run.gpx", "application/xml", "<gpx></gpx>".getBytes());

            List<GpsPoint> gpsPoints = List.of(
                    GpsPoint.builder().latitude(52.2297).longitude(21.0122).build(),
                    GpsPoint.builder().latitude(52.2350).longitude(21.0200).build(),
                    GpsPoint.builder().latitude(52.2400).longitude(21.0300).build()
            );
            ParsedActivity parsed = ParsedActivity.builder()
                    .title("With GPS")
                    .activityType(ActivityType.RUN)
                    .distance(1.0)
                    .duration(600)
                    .gpsPoints(gpsPoints)
                    .build();

            when(gpxParser.supports("gpx")).thenReturn(true);
            when(gpxParser.parse(any(InputStream.class))).thenReturn(parsed);

            ArgumentCaptor<ActivityRequest> reqCaptor = ArgumentCaptor.forClass(ActivityRequest.class);
            when(activityService.createActivity(eq(1L), reqCaptor.capture()))
                    .thenReturn(Activity.builder().id(1L).build());

            importService.importActivity(1L, file, null, null, false);

            // Polyline should be the encoded (compact, non-JSON) form
            ActivityRequest req = reqCaptor.getValue();
            assertThat(req.getRoutePolyline()).isNotBlank();
            assertThat(req.getRoutePolyline()).doesNotStartWith("[");
        }

        @Test
        @DisplayName("Should serialize aligned elevation + timestamp streams into the request")
        void shouldSerializeAlignedStreams() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "run.gpx", "application/xml", "<gpx></gpx>".getBytes());

            List<GpsPoint> gpsPoints = List.of(
                    GpsPoint.builder().latitude(52.2297).longitude(21.0122).build(),
                    GpsPoint.builder().latitude(52.2350).longitude(21.0200).build()
            );
            ParsedActivity parsed = ParsedActivity.builder()
                    .title("Streams")
                    .activityType(ActivityType.RUN)
                    .distance(1.0)
                    .duration(600)
                    .gpsPoints(gpsPoints)
                    .elevations(List.of(100.0, 105.5))
                    .pointTimestampsEpochSec(List.of(1_700_000_000L, 1_700_000_060L))
                    .build();

            when(gpxParser.supports("gpx")).thenReturn(true);
            when(gpxParser.parse(any(InputStream.class))).thenReturn(parsed);

            ArgumentCaptor<ActivityRequest> reqCaptor = ArgumentCaptor.forClass(ActivityRequest.class);
            when(activityService.createActivity(eq(1L), reqCaptor.capture()))
                    .thenReturn(Activity.builder().id(1L).build());

            importService.importActivity(1L, file, null, null, false);

            ActivityRequest req = reqCaptor.getValue();
            assertThat(req.getElevationProfile()).isEqualTo("[100.0,105.5]");
            assertThat(req.getPointTimestamps()).isEqualTo("[1700000000,1700000060]");
        }

        @Test
        @DisplayName("Should apply title override when provided")
        void shouldApplyTitleOverride() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "run.gpx", "application/xml", "<gpx></gpx>".getBytes());
            ParsedActivity parsed = ParsedActivity.builder()
                    .title("Parsed Title")
                    .distance(1.0)
                    .duration(300)
                    .gpsPoints(List.of())
                    .build();

            when(gpxParser.supports("gpx")).thenReturn(true);
            when(gpxParser.parse(any(InputStream.class))).thenReturn(parsed);

            ArgumentCaptor<ActivityRequest> reqCaptor = ArgumentCaptor.forClass(ActivityRequest.class);
            when(activityService.createActivity(eq(1L), reqCaptor.capture()))
                    .thenReturn(Activity.builder().id(1L).build());

            importService.importActivity(1L, file, "Overridden Title", ActivityType.HIKE, true);

            ActivityRequest req = reqCaptor.getValue();
            assertThat(req.getTitle()).isEqualTo("Overridden Title");
            assertThat(req.getActivityType()).isEqualTo(ActivityType.HIKE);
            assertThat(req.isPrivate()).isTrue();
        }

        @Test
        @DisplayName("Should wrap IllegalArgumentException from parser as ApiRequestException")
        void shouldWrapParserIllegalArgumentException() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "run.gpx", "application/xml", "garbage".getBytes());
            when(gpxParser.supports("gpx")).thenReturn(true);
            when(gpxParser.parse(any(InputStream.class)))
                    .thenThrow(new IllegalArgumentException("bad header"));

            assertThatThrownBy(() -> importService.importActivity(1L, file, null, null, false))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessageContaining(IMPORT_PARSE_ERROR)
                    .hasMessageContaining("bad header");
        }

        @Test
        @DisplayName("Should wrap generic parser exceptions as ApiRequestException")
        void shouldWrapGenericParserException() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "run.gpx", "application/xml", "garbage".getBytes());
            when(gpxParser.supports("gpx")).thenReturn(true);
            when(gpxParser.parse(any(InputStream.class)))
                    .thenThrow(new RuntimeException("kaboom"));

            assertThatThrownBy(() -> importService.importActivity(1L, file, null, null, false))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(IMPORT_PARSE_ERROR);
        }
    }

    @Nested
    @DisplayName("listSampleFiles Tests")
    class ListSampleFilesTests {

        @Test
        @DisplayName("Should return a non-empty list with all expected sample filenames")
        void shouldReturnAllExpectedSamples() {
            List<SampleFileInfo> samples = importService.listSampleFiles();

            assertThat(samples).isNotEmpty();
            assertThat(samples).extracting(SampleFileInfo::getFilename)
                    .containsExactlyInAnyOrder(
                            "morning_run_5k.gpx",
                            "weekend_cycling_20k.gpx",
                            "mountain_hike.gpx",
                            "interval_training.tcx",
                            "cycling_workout.tcx",
                            "Night_Run.fit"
                    );
            assertThat(samples).allSatisfy(s -> {
                assertThat(s.getFormat()).isNotBlank();
                assertThat(s.getActivityType()).isNotBlank();
                assertThat(s.getDescription()).isNotBlank();
                assertThat(s.getDownloadUrl()).startsWith("/api/v1/import/samples/");
            });
        }
    }

    @Nested
    @DisplayName("getSampleFile Tests")
    class GetSampleFileTests {

        @Test
        @DisplayName("Should reject filenames containing '..' (path traversal)")
        void shouldRejectPathTraversalDots() {
            assertThatThrownBy(() -> importService.getSampleFile("../etc/passwd"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(INVALID_SAMPLE_FILENAME)
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should reject filenames containing '/' (path separator)")
        void shouldRejectPathTraversalSlash() {
            assertThatThrownBy(() -> importService.getSampleFile("subdir/run.gpx"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(INVALID_SAMPLE_FILENAME);
        }

        @Test
        @DisplayName("Should reject null filename")
        void shouldRejectNullFilename() {
            assertThatThrownBy(() -> importService.getSampleFile(null))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(INVALID_SAMPLE_FILENAME);
        }

        @Test
        @DisplayName("Should throw SAMPLE_FILE_NOT_FOUND for a safe filename that does not exist")
        void shouldThrowNotFoundForMissingFile() {
            assertThatThrownBy(() -> importService.getSampleFile("does_not_exist.gpx"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(SAMPLE_FILE_NOT_FOUND)
                    .extracting("status")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should return the resource for a known sample file")
        void shouldReturnResourceForKnownSample() {
            Resource resource = importService.getSampleFile("morning_run_5k.gpx");

            assertThat(resource).isNotNull();
            assertThat(resource.exists()).isTrue();
        }
    }
}
