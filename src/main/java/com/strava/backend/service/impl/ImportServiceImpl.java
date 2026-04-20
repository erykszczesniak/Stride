package com.strava.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.domain.Activity;
import com.strava.backend.domain.User;
import com.strava.backend.dto.importactivity.SampleFileInfo;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.service.ImportService;
import com.strava.backend.service.UserService;
import com.strava.backend.service.importer.ActivityFileParser;
import com.strava.backend.service.importer.GeoUtils;
import com.strava.backend.service.importer.ParsedActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.strava.backend.constants.ErrorMessage.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportServiceImpl implements ImportService {

    private static final long MAX_IMPORT_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("gpx", "tcx", "fit");

    private static final String SAMPLE_DIR = "sample-activities/";

    private final List<ActivityFileParser> parsers;
    private final ActivityRepository activityRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public Activity importActivity(Long userId, MultipartFile file, String titleOverride,
                                   ActivityType activityTypeOverride, boolean isPrivate) {
        validateFile(file);

        String extension = getFileExtension(file.getOriginalFilename());
        ActivityFileParser parser = parsers.stream()
                .filter(p -> p.supports(extension))
                .findFirst()
                .orElseThrow(() -> new ApiRequestException(UNSUPPORTED_FILE_FORMAT, HttpStatus.BAD_REQUEST));

        ParsedActivity parsed;
        try {
            parsed = parser.parse(file.getInputStream());
        } catch (IllegalArgumentException e) {
            throw new ApiRequestException(IMPORT_PARSE_ERROR + ": " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed to parse imported file: {}", file.getOriginalFilename(), e);
            throw new ApiRequestException(IMPORT_PARSE_ERROR, HttpStatus.BAD_REQUEST);
        }

        User user = userService.getUserById(userId);

        Activity activity = Activity.builder()
                .title(titleOverride != null && !titleOverride.isBlank() ? titleOverride : parsed.getTitle())
                .description(parsed.getDescription())
                .activityType(activityTypeOverride != null ? activityTypeOverride
                        : parsed.getActivityType() != null ? parsed.getActivityType() : ActivityType.OTHER)
                .distance(parsed.getDistance() != null ? parsed.getDistance() : 0.0)
                .duration(parsed.getDuration() != null ? parsed.getDuration() : 0)
                .elevationGain(parsed.getElevationGain())
                .calories(parsed.getCalories())
                .averageSpeed(parsed.getAverageSpeed())
                .maxSpeed(parsed.getMaxSpeed())
                .averageHeartRate(parsed.getAverageHeartRate())
                .maxHeartRate(parsed.getMaxHeartRate())
                .averageCadence(parsed.getAverageCadence())
                .averagePower(parsed.getAveragePower())
                .startTime(parsed.getStartTime() != null ? parsed.getStartTime() : LocalDateTime.now())
                .startLocation(parsed.getStartLocation())
                .endLocation(parsed.getEndLocation())
                .isPrivate(isPrivate)
                .user(user)
                .build();

        // Encode GPS points to polyline
        if (parsed.getGpsPoints() != null && !parsed.getGpsPoints().isEmpty()) {
            activity.setRoutePolyline(GeoUtils.encodePolyline(parsed.getGpsPoints()));
        }

        // Persist per-point elevation series as JSON (or null if the source had no altitude data).
        // We tolerate serialization failures — elevation profile is a nice-to-have, not a
        // reason to reject an otherwise valid import.
        if (parsed.getElevations() != null && !parsed.getElevations().isEmpty()) {
            try {
                activity.setElevationProfile(objectMapper.writeValueAsString(parsed.getElevations()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize elevation profile for import; leaving null", e);
            }
        }

        Activity saved = activityRepository.save(activity);
        log.info("Imported {} activity from {} file for user {}: distance={}km, duration={}s, gpsPoints={}",
                saved.getActivityType(), extension, userId, saved.getDistance(), saved.getDuration(),
                parsed.getGpsPoints() != null ? parsed.getGpsPoints().size() : 0);

        return saved;
    }

    @Override
    public List<SampleFileInfo> listSampleFiles() {
        List<SampleFileInfo> samples = new ArrayList<>();

        samples.add(SampleFileInfo.builder()
                .filename("morning_run_5k.gpx")
                .format("GPX")
                .activityType("Running")
                .description("5K morning run around Łazienki Park in Warsaw with heart rate data")
                .downloadUrl("/api/v1/import/samples/morning_run_5k.gpx")
                .build());

        samples.add(SampleFileInfo.builder()
                .filename("weekend_cycling_20k.gpx")
                .format("GPX")
                .activityType("Cycling")
                .description("20K weekend cycling route through Amsterdam canals")
                .downloadUrl("/api/v1/import/samples/weekend_cycling_20k.gpx")
                .build());

        samples.add(SampleFileInfo.builder()
                .filename("mountain_hike.gpx")
                .format("GPX")
                .activityType("Hiking")
                .description("Mountain hike to Morskie Oko in the Polish Tatras with elevation data")
                .downloadUrl("/api/v1/import/samples/mountain_hike.gpx")
                .build());

        samples.add(SampleFileInfo.builder()
                .filename("interval_training.tcx")
                .format("TCX")
                .activityType("Running")
                .description("Track interval training session with heart rate and cadence data")
                .downloadUrl("/api/v1/import/samples/interval_training.tcx")
                .build());

        samples.add(SampleFileInfo.builder()
                .filename("cycling_workout.tcx")
                .format("TCX")
                .activityType("Cycling")
                .description("Indoor cycling workout with power and cadence data")
                .downloadUrl("/api/v1/import/samples/cycling_workout.tcx")
                .build());

        samples.add(SampleFileInfo.builder()
                .filename("Night_Run.fit")
                .format("FIT")
                .activityType("Running")
                .description("Real night run captured by a Decathlon Kiprun GPS 900 watch — ~6 km around Warsaw with full GPS, heart rate, and cadence data")
                .downloadUrl("/api/v1/import/samples/Night_Run.fit")
                .build());

        return samples;
    }

    @Override
    public Resource getSampleFile(String filename) {
        if (filename == null || filename.contains("..") || filename.contains("/")) {
            throw new ApiRequestException(INVALID_SAMPLE_FILENAME, HttpStatus.BAD_REQUEST);
        }

        Resource resource = new ClassPathResource(SAMPLE_DIR + filename);
        if (!resource.exists()) {
            throw new ApiRequestException(SAMPLE_FILE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        return resource;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiRequestException(IMPORT_FILE_EMPTY, HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_IMPORT_FILE_SIZE) {
            throw new ApiRequestException(IMPORT_FILE_TOO_LARGE, HttpStatus.BAD_REQUEST);
        }
        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new ApiRequestException(UNSUPPORTED_FILE_FORMAT, HttpStatus.BAD_REQUEST);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
