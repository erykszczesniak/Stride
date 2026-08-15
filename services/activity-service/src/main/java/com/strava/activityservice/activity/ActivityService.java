package com.strava.activityservice.activity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.activityservice.domain.Activity;
import com.strava.activityservice.domain.ActivityRepository;
import com.strava.activityservice.domain.ActivityType;
import com.strava.activityservice.event.ActivityEventPublisher;
import com.strava.activityservice.exception.ApiRequestException;
import com.strava.activityservice.pr.PersonalRecordService;
import com.strava.activityservice.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.strava.activityservice.constants.ErrorMessage.*;

/**
 * Business logic ported from main's ActivityServiceImpl. Deviations forced by
 * the service split: likes/comments/feed live in social-service, gear lands
 * in a later phase, and cross-service effects go out as Kafka events instead
 * of in-process Spring events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private static final long MAX_PHOTO_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int MAX_PHOTOS_PER_ACTIVITY = 5;
    private static final ObjectMapper STREAM_PARSER = new ObjectMapper();
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final ActivityRepository activityRepository;
    private final PersonalRecordService personalRecordService;
    private final ActivityEventPublisher eventPublisher;

    @Value("${app.upload.dir:uploads/photos}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public Activity getActivityById(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Page<Activity> getActivitiesByUserId(Long userId, Pageable pageable) {
        return activityRepository.findByUserId(userId, pageable);
    }

    @Transactional
    public Activity createActivity(Long userId, ActivityRequest request) {
        validateStreamAlignment(request);

        Activity activity = Activity.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .activityType(request.getActivityType())
                .distance(request.getDistance())
                .duration(request.getDuration())
                .elevationGain(request.getElevationGain())
                .calories(request.getCalories())
                .averageSpeed(request.getAverageSpeed())
                .maxSpeed(request.getMaxSpeed())
                .averageHeartRate(request.getAverageHeartRate())
                .maxHeartRate(request.getMaxHeartRate())
                .averageCadence(request.getAverageCadence())
                .averagePower(request.getAveragePower())
                .startTime(request.getStartTime())
                .startLocation(request.getStartLocation())
                .endLocation(request.getEndLocation())
                .routePolyline(request.getRoutePolyline())
                .pointTimestamps(request.getPointTimestamps())
                .elevationProfile(request.getElevationProfile())
                .isPrivate(request.isPrivate())
                .build();

        Activity saved = activityRepository.save(activity);
        personalRecordService.checkAndUpdatePRs(saved);

        eventPublisher.publishActivityCreated(saved);

        return saved;
    }

    @Transactional
    public Activity updateActivity(Long activityId, Long userId, ActivityRequest request) {
        validateStreamAlignment(request);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (!activity.getUserId().equals(userId)) {
            throw new ApiRequestException(ACTIVITY_ACCESS_DENIED, HttpStatus.FORBIDDEN);
        }

        // Snapshot privacy so we can detect a flip and notify the social read model.
        boolean wasPrivate = activity.isPrivate();

        activity.setTitle(request.getTitle());
        activity.setDescription(request.getDescription());
        activity.setActivityType(request.getActivityType());
        activity.setDistance(request.getDistance());
        activity.setDuration(request.getDuration());
        activity.setElevationGain(request.getElevationGain());
        activity.setCalories(request.getCalories());
        activity.setAverageSpeed(request.getAverageSpeed());
        activity.setPrivate(request.isPrivate());

        // Only update optional fields if provided (preserve existing GPS/route data)
        if (request.getMaxSpeed() != null) activity.setMaxSpeed(request.getMaxSpeed());
        if (request.getAverageHeartRate() != null) activity.setAverageHeartRate(request.getAverageHeartRate());
        if (request.getMaxHeartRate() != null) activity.setMaxHeartRate(request.getMaxHeartRate());
        if (request.getAverageCadence() != null) activity.setAverageCadence(request.getAverageCadence());
        if (request.getAveragePower() != null) activity.setAveragePower(request.getAveragePower());
        if (request.getStartTime() != null) activity.setStartTime(request.getStartTime());
        if (request.getStartLocation() != null) activity.setStartLocation(request.getStartLocation());
        if (request.getEndLocation() != null) activity.setEndLocation(request.getEndLocation());
        if (request.getRoutePolyline() != null) {
            // A new route replaces its aligned streams too: keeping the old
            // pointTimestamps/elevationProfile against a different polyline
            // would silently misalign every consumer (splits, elevation card).
            activity.setRoutePolyline(request.getRoutePolyline());
            activity.setPointTimestamps(request.getPointTimestamps());
            activity.setElevationProfile(request.getElevationProfile());
        }

        Activity saved = activityRepository.save(activity);

        personalRecordService.checkAndUpdatePRs(saved);

        eventPublisher.publishActivityUpdated(saved);
        if (wasPrivate != saved.isPrivate()) {
            eventPublisher.publishActivityVisibilityChanged(saved);
        }

        return saved;
    }

    /**
     * GPS streams are only meaningful when aligned 1:1 with the decoded route
     * polyline — every consumer (accurate splits, elevation charts, the
     * pipeline facade) indexes them positionally. Reject writes that would
     * break that invariant instead of storing silently-misaligned data.
     */
    private void validateStreamAlignment(ActivityRequest request) {
        boolean hasTimestamps = StringUtils.hasText(request.getPointTimestamps());
        boolean hasElevation = StringUtils.hasText(request.getElevationProfile());
        if (!hasTimestamps && !hasElevation) {
            return;
        }

        List<double[]> coords = GeoUtils.decodePolyline(request.getRoutePolyline());
        if (coords.size() < 2) {
            throw new ApiRequestException(STREAMS_WITHOUT_ROUTE, HttpStatus.BAD_REQUEST);
        }

        if (hasTimestamps) {
            long[] timestamps = parseLongArray(request.getPointTimestamps());
            if (timestamps == null || timestamps.length != coords.size() || !isNonDecreasing(timestamps)) {
                throw new ApiRequestException(MISALIGNED_POINT_TIMESTAMPS, HttpStatus.BAD_REQUEST);
            }
        }

        if (hasElevation) {
            Double[] elevation = parseDoubleArray(request.getElevationProfile());
            if (elevation == null || elevation.length != coords.size()) {
                throw new ApiRequestException(MISALIGNED_ELEVATION_PROFILE, HttpStatus.BAD_REQUEST);
            }
        }
    }

    private long[] parseLongArray(String json) {
        try {
            Long[] boxed = STREAM_PARSER.readValue(json, Long[].class);
            long[] result = new long[boxed.length];
            for (int i = 0; i < boxed.length; i++) {
                if (boxed[i] == null) return null;
                result[i] = boxed[i];
            }
            return result;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Double[] parseDoubleArray(String json) {
        try {
            return STREAM_PARSER.readValue(json, Double[].class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static boolean isNonDecreasing(long[] values) {
        for (int i = 1; i < values.length; i++) {
            if (values[i] < values[i - 1]) return false;
        }
        return true;
    }

    @Transactional
    public void deleteActivity(Long activityId, Long userId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (!activity.getUserId().equals(userId)) {
            throw new ApiRequestException(ACTIVITY_ACCESS_DENIED, HttpStatus.FORBIDDEN);
        }

        deleteWithPersonalRecords(activity);
    }

    /**
     * Shared by the owner and the admin delete paths. PR rows referencing the
     * activity go with it — the monolith did the same via the
     * {@code activity_id ... ON DELETE CASCADE} FK (no recompute afterwards).
     */
    @Transactional
    public void deleteWithPersonalRecords(Activity activity) {
        Long activityId = activity.getId();
        Long userId = activity.getUserId();

        personalRecordService.deleteByActivityId(activityId);
        activityRepository.delete(activity);

        eventPublisher.publishActivityDeleted(activityId, userId);
    }

    @Transactional(readOnly = true)
    public ActivityStatsResponse getMonthlyStats(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);

        LocalDateTime startOfPrevMonth = startOfMonth.minusMonths(1);
        LocalDateTime endOfPrevMonth = startOfMonth.minusSeconds(1);

        // Single native query to get all aggregated stats for the current month
        Object[] rawStats = activityRepository.getAggregatedStatsByUserAndDateRange(userId, startOfMonth, endOfMonth);
        // Native query returns Object[] where each element is a column value
        Object[] stats = (rawStats != null && rawStats.length > 0 && rawStats[0] instanceof Object[])
                ? (Object[]) rawStats[0] : rawStats;
        double totalDistance = stats != null ? ((Number) stats[0]).doubleValue() : 0.0;
        long totalDuration = stats != null ? ((Number) stats[1]).longValue() : 0L;
        int totalCalories = stats != null ? ((Number) stats[2]).intValue() : 0;
        int totalElevation = stats != null ? ((Number) stats[3]).intValue() : 0;
        int totalActivities = stats != null ? ((Number) stats[4]).intValue() : 0;

        Object[] prevRawStats = activityRepository.getAggregatedStatsByUserAndDateRange(userId, startOfPrevMonth, endOfPrevMonth);
        Object[] prevStats = (prevRawStats != null && prevRawStats.length > 0 && prevRawStats[0] instanceof Object[])
                ? (Object[]) prevRawStats[0] : prevRawStats;
        double prevDistance = prevStats != null ? ((Number) prevStats[0]).doubleValue() : 0.0;
        long prevDuration = prevStats != null ? ((Number) prevStats[1]).longValue() : 0L;
        int prevCalories = prevStats != null ? ((Number) prevStats[2]).intValue() : 0;
        int prevActivities = prevStats != null ? ((Number) prevStats[4]).intValue() : 0;

        String trend = calculateTrend(totalDistance, prevDistance);

        return ActivityStatsResponse.builder()
                .totalDistance(totalDistance)
                .totalDuration(totalDuration)
                .totalCalories(totalCalories)
                .totalActivities(totalActivities)
                .totalElevationGain(totalElevation)
                .averageDistance(totalActivities > 0 ? totalDistance / totalActivities : 0.0)
                .averageDuration(totalActivities > 0 ? (double) totalDuration / totalActivities : 0.0)
                .trendPercentage(trend)
                .distanceTrendPercentage(calculateTrendNumeric(totalDistance, prevDistance))
                .durationTrendPercentage(calculateTrendNumeric((double) totalDuration, (double) prevDuration))
                .caloriesTrendPercentage(calculateTrendNumeric((double) totalCalories, (double) prevCalories))
                .activitiesTrendPercentage(calculateTrendNumeric((double) totalActivities, (double) prevActivities))
                .build();
    }

    @Transactional(readOnly = true)
    public WeeklyStatsResponse getWeeklyStats(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfWeek = startOfWeek.plusWeeks(1).minusSeconds(1);

        LocalDateTime startOfPrevWeek = startOfWeek.minusWeeks(1);
        LocalDateTime endOfPrevWeek = startOfWeek.minusSeconds(1);

        Double prevWeekDistance = activityRepository.sumDistanceByUserAndDateRange(userId, startOfPrevWeek, endOfPrevWeek)
                .orElse(0.0);

        // Single query to get daily aggregated stats for the whole week
        List<Object[]> dailyResults = activityRepository.getDailyStatsByUserAndDateRange(userId, startOfWeek, endOfWeek);

        // PostgreSQL EXTRACT(DOW FROM ...) returns: 0=Sunday, 1=Monday, ..., 6=Saturday
        // Map to our array index: Mon=0, Tue=1, ..., Sun=6
        // So DOW 1->0, 2->1, 3->2, 4->3, 5->4, 6->5, 0->6
        Double[] distances = new Double[7];
        Integer[] durations = new Integer[7];
        Integer[] activities = new Integer[7];
        for (int i = 0; i < 7; i++) {
            distances[i] = 0.0;
            durations[i] = 0;
            activities[i] = 0;
        }

        double totalDistance = 0.0;
        for (Object[] row : dailyResults) {
            int dow = ((Number) row[0]).intValue(); // PostgreSQL DOW: 0=Sun, 1=Mon, ..., 6=Sat
            int index = dow == 0 ? 6 : dow - 1; // Convert to Mon=0 .. Sun=6
            double dist = ((Number) row[1]).doubleValue();
            int dur = ((Number) row[2]).intValue();
            int act = ((Number) row[3]).intValue();
            distances[index] = dist;
            durations[index] = dur;
            activities[index] = act;
            totalDistance += dist;
        }

        String trend = calculateTrend(totalDistance, prevWeekDistance);

        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        List<WeeklyStatsResponse.DailyStats> dailyStats = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            dailyStats.add(WeeklyStatsResponse.DailyStats.builder()
                    .dayName(dayNames[i])
                    .distance(distances[i])
                    .duration(durations[i])
                    .activities(activities[i])
                    .build());
        }

        return WeeklyStatsResponse.builder()
                .totalDistance(totalDistance)
                .trendPercentage(trend)
                .dailyStats(dailyStats)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<Activity> searchUserActivities(Long userId, String searchText, ActivityType activityType, Pageable pageable) {
        if (activityType != null) {
            return activityRepository.searchUserActivitiesByType(userId, searchText, activityType, pageable);
        }
        return activityRepository.searchUserActivities(userId, searchText, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Activity> getActivitiesByUserIdAndType(Long userId, ActivityType activityType, Pageable pageable) {
        return activityRepository.findByUserIdAndActivityType(userId, activityType, pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserTotalStats(Long userId) {
        int totalActivities = activityRepository.countByUserId(userId);
        Double totalDistance = activityRepository.sumDistanceByUserId(userId);
        Long totalDuration = activityRepository.sumDurationByUserId(userId);
        Long totalElevation = activityRepository.sumElevationByUserId(userId);
        Long totalCalories = activityRepository.sumCaloriesByUserId(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalActivities", totalActivities);
        stats.put("totalDistanceKm", totalDistance != null ? totalDistance : 0.0);
        stats.put("totalDurationSeconds", totalDuration != null ? totalDuration : 0L);
        stats.put("totalElevationMeters", totalElevation != null ? totalElevation : 0L);
        stats.put("totalCalories", totalCalories != null ? totalCalories : 0L);
        return stats;
    }

    @Transactional
    public List<String> uploadPhotos(Long activityId, Long userId, List<MultipartFile> files) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (!activity.getUserId().equals(userId)) {
            throw new ApiRequestException(ACTIVITY_ACCESS_DENIED, HttpStatus.FORBIDDEN);
        }

        int currentCount = activity.getPhotos().size();
        if (currentCount + files.size() > MAX_PHOTOS_PER_ACTIVITY) {
            throw new ApiRequestException(PHOTO_LIMIT_EXCEEDED, HttpStatus.BAD_REQUEST);
        }

        for (MultipartFile file : files) {
            if (file.getSize() > MAX_PHOTO_SIZE) {
                throw new ApiRequestException(PHOTO_TOO_LARGE, HttpStatus.BAD_REQUEST);
            }
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
                throw new ApiRequestException(INVALID_PHOTO_TYPE, HttpStatus.BAD_REQUEST);
            }
        }

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            List<String> newUrls = new ArrayList<>();
            for (MultipartFile file : files) {
                String extension = getFileExtension(file.getOriginalFilename());
                String filename = UUID.randomUUID() + extension;
                Path filePath = uploadPath.resolve(filename);
                Files.copy(file.getInputStream(), filePath);
                String photoUrl = "/uploads/photos/" + filename;
                activity.getPhotos().add(photoUrl);
                newUrls.add(photoUrl);
            }

            activityRepository.save(activity);
            return activity.getPhotos();
        } catch (IOException e) {
            log.error("Failed to upload photos for activity {}", activityId, e);
            throw new ApiRequestException("Failed to upload photos", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public void deletePhoto(Long activityId, Long userId, int photoIndex) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (!activity.getUserId().equals(userId)) {
            throw new ApiRequestException(ACTIVITY_ACCESS_DENIED, HttpStatus.FORBIDDEN);
        }

        if (photoIndex < 0 || photoIndex >= activity.getPhotos().size()) {
            throw new ApiRequestException(INVALID_PHOTO_INDEX, HttpStatus.BAD_REQUEST);
        }

        String photoUrl = activity.getPhotos().get(photoIndex);
        activity.getPhotos().remove(photoIndex);
        activityRepository.save(activity);

        // Delete file from disk
        try {
            String filename = photoUrl.substring(photoUrl.lastIndexOf('/') + 1);
            Path filePath = Paths.get(uploadDir).resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete photo file: {}", photoUrl, e);
        }
    }

    /** Aggregates for the cluster-internal /internal/activities/stats/range API. */
    @Transactional(readOnly = true)
    public Map<String, Object> getRangeStats(Long userId, LocalDateTime from, LocalDateTime to) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDistanceKm", activityRepository.sumDistanceByUserAndDateRange(userId, from, to).orElse(0.0));
        stats.put("totalDurationSeconds", activityRepository.sumDurationByUserAndDateRange(userId, from, to).orElse(0L));
        stats.put("totalElevationGain", activityRepository.sumElevationByUserAndDateRange(userId, from, to).orElse(0L));
        stats.put("totalCalories", activityRepository.sumCaloriesByUserAndDateRange(userId, from, to).orElse(0L));
        stats.put("activityCount", activityRepository.countByUserAndDateRange(userId, from, to));
        return stats;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    private Double calculateTrendNumeric(Double current, Double previous) {
        if (previous == null || previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        double percentage = ((current - previous) / previous) * 100;
        return Math.round(percentage * 10.0) / 10.0;
    }

    private String calculateTrend(Double current, Double previous) {
        if (previous == null || previous == 0) {
            return current > 0 ? "+100%" : "0%";
        }
        double percentage = ((current - previous) / previous) * 100;
        return String.format("%s%.1f%%", percentage >= 0 ? "+" : "", percentage);
    }
}
