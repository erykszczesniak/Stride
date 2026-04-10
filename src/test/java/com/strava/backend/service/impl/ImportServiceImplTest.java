package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.User;
import com.strava.backend.dto.importactivity.SampleFileInfo;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.service.UserService;
import com.strava.backend.service.importer.ActivityFileParser;
import com.strava.backend.service.importer.GpsPoint;
import com.strava.backend.service.importer.ParsedActivity;
import com.strava.backend.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

import static com.strava.backend.constants.ErrorMessage.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImportServiceImpl Unit Tests")
class ImportServiceImplTest {

    @Mock
    private ActivityFileParser gpxParser;

    @Mock
    private ActivityFileParser fitParser;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserService userService;

    private ImportServiceImpl importService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createUser(1L, "john@example.com", "John", "Doe");
        importService = new ImportServiceImpl(List.of(gpxParser, fitParser), activityRepository, userService);
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
    @DisplayName("importActivity Parser Selection Tests")
    class ParserSelectionTests {

        @Test
        @DisplayName("Should pick the GPX parser for a .gpx file and save the activity")
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
            when(userService.getUserById(1L)).thenReturn(testUser);
            when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> {
                Activity a = inv.getArgument(0);
                a.setId(99L);
                return a;
            });

            Activity result = importService.importActivity(1L, file, null, null, false);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(99L);
            assertThat(result.getTitle()).isEqualTo("Morning Run");
            assertThat(result.getActivityType()).isEqualTo(ActivityType.RUN);
            assertThat(result.getDistance()).isEqualTo(5.0);
            assertThat(result.getDuration()).isEqualTo(1500);
            verify(gpxParser).parse(any(InputStream.class));
            verify(fitParser, never()).parse(any());
            verify(activityRepository).save(any(Activity.class));
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
        @DisplayName("Should store GPS points as encoded polyline on the saved activity")
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
            when(userService.getUserById(1L)).thenReturn(testUser);
            when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

            Activity result = importService.importActivity(1L, file, null, null, false);

            // Polyline should be the encoded (compact, non-JSON) form
            assertThat(result.getRoutePolyline()).isNotBlank();
            assertThat(result.getRoutePolyline()).doesNotStartWith("[");
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
            when(userService.getUserById(1L)).thenReturn(testUser);
            when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

            Activity result = importService.importActivity(1L, file, "Overridden Title", ActivityType.HIKE, true);

            assertThat(result.getTitle()).isEqualTo("Overridden Title");
            assertThat(result.getActivityType()).isEqualTo(ActivityType.HIKE);
            assertThat(result.isPrivate()).isTrue();
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
