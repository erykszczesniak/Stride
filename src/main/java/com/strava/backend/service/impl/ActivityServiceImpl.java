package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.Gear;
import com.strava.backend.domain.User;
import com.strava.backend.dto.activity.ActivityRequest;
import com.strava.backend.dto.activity.ActivityStatsResponse;
import com.strava.backend.dto.activity.WeeklyStatsResponse;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.enums.NotificationType;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.service.ActivityService;
import com.strava.backend.service.GearService;
import com.strava.backend.service.NotificationService;
import com.strava.backend.service.PersonalRecordService;
import com.strava.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.stream.Collectors;

import static com.strava.backend.constants.ErrorMessage.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private static final long MAX_PHOTO_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int MAX_PHOTOS_PER_ACTIVITY = 5;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final ActivityRepository activityRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final PersonalRecordService personalRecordService;
    private final GearService gearService;

    @Value("${app.upload.dir:uploads/photos}")
    private String uploadDir;

    @Override
    @Transactional(readOnly = true)
    public Activity getActivityById(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Activity> getActivitiesByUserId(Long userId, Pageable pageable) {
        return activityRepository.findByUserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Activity> getRecentActivitiesByUserId(Long userId, int limit) {
        return activityRepository.findByUserId(userId, PageRequest.of(0, limit))
                .getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Activity> getFeedActivities(Long userId, Pageable pageable) {
        User user = userService.getUserById(userId);
        List<Long> followingIds = user.getFollowing().stream()
                .map(User::getId)
                .collect(Collectors.toList());
        followingIds.add(userId);
        return activityRepository.findByUserIdIn(followingIds, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Activity> getPublicActivities(Pageable pageable) {
        return activityRepository.findByIsPrivateFalse(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Activity> getPublicActivitiesVisibleTo(Long viewerId, Pageable pageable) {
        return activityRepository.findPublicActivitiesVisibleTo(viewerId, pageable);
    }

    @Override
    @Transactional
    public Activity createActivity(Long userId, ActivityRequest request) {
        User user = userService.getUserById(userId);

        Gear gear = resolveGear(request.getGearId(), userId, request.getActivityType());

        Activity activity = Activity.builder()
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
                .isPrivate(request.isPrivate())
                .user(user)
                .gear(gear)
                .build();

        Activity saved = activityRepository.save(activity);
        if (gear != null && saved.getDistance() != null) {
            gearService.incrementGearMileage(gear.getId(), saved.getDistance());
        }
        personalRecordService.checkAndUpdatePRs(saved);
        return saved;
    }

    @Override
    @Transactional
    public Activity updateActivity(Long activityId, Long userId, ActivityRequest request) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (!activity.getUser().getId().equals(userId)) {
            throw new ApiRequestException(ACTIVITY_ACCESS_DENIED, HttpStatus.FORBIDDEN);
        }

        // Snapshot old gear/distance before mutating the activity — we need
        // both to reverse the old mileage entry before applying the new one.
        Gear oldGear = activity.getGear();
        Double oldDistance = activity.getDistance();

        activity.setTitle(request.getTitle());
        activity.setDescription(request.getDescription());
        activity.setActivityType(request.getActivityType());
        activity.setDistance(request.getDistance());
        activity.setDuration(request.getDuration());
        activity.setElevationGain(request.getElevationGain());
        activity.setCalories(request.getCalories());
        activity.setAverageSpeed(request.getAverageSpeed());
        activity.setPrivate(request.isPrivate());

        Gear newGear = resolveGear(request.getGearId(), userId, request.getActivityType());
        activity.setGear(newGear);

        // Only update optional fields if provided (preserve existing GPS/route data)
        if (request.getMaxSpeed() != null) activity.setMaxSpeed(request.getMaxSpeed());
        if (request.getAverageHeartRate() != null) activity.setAverageHeartRate(request.getAverageHeartRate());
        if (request.getMaxHeartRate() != null) activity.setMaxHeartRate(request.getMaxHeartRate());
        if (request.getAverageCadence() != null) activity.setAverageCadence(request.getAverageCadence());
        if (request.getAveragePower() != null) activity.setAveragePower(request.getAveragePower());
        if (request.getStartTime() != null) activity.setStartTime(request.getStartTime());
        if (request.getStartLocation() != null) activity.setStartLocation(request.getStartLocation());
        if (request.getEndLocation() != null) activity.setEndLocation(request.getEndLocation());
        if (request.getRoutePolyline() != null) activity.setRoutePolyline(request.getRoutePolyline());

        Activity saved = activityRepository.save(activity);

        // Adjust gear mileage after the save so we don't leave gear counters
        // in an inconsistent state if the save itself fails.
        adjustGearMileageOnUpdate(oldGear, oldDistance, newGear, saved.getDistance());

        personalRecordService.checkAndUpdatePRs(saved);
        return saved;
    }

    @Override
    @Transactional
    public void deleteActivity(Long activityId, Long userId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (!activity.getUser().getId().equals(userId)) {
            throw new ApiRequestException(ACTIVITY_ACCESS_DENIED, HttpStatus.FORBIDDEN);
        }

        Gear gear = activity.getGear();
        Double distance = activity.getDistance();

        activityRepository.delete(activity);

        if (gear != null && distance != null) {
            gearService.decrementGearMileage(gear.getId(), distance);
        }
    }

    @Override
    @Transactional
    public void likeActivity(Long activityId, Long userId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));
        User user = userService.getUserById(userId);

        if (activity.getLikedBy().contains(user)) {
            throw new ApiRequestException("Activity already liked", HttpStatus.BAD_REQUEST);
        }

        activity.getLikedBy().add(user);
        activityRepository.save(activity);

        notificationService.createNotification(
                activity.getUser(),
                user,
                NotificationType.LIKE,
                user.getFirstName() + " " + user.getLastName() + " liked your activity \"" + activity.getTitle() + "\"",
                activityId,
                "ACTIVITY"
        );
    }

    @Override
    @Transactional
    public void unlikeActivity(Long activityId, Long userId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));
        User user = userService.getUserById(userId);

        if (!activity.getLikedBy().contains(user)) {
            throw new ApiRequestException("Activity not liked", HttpStatus.BAD_REQUEST);
        }

        activity.getLikedBy().remove(user);
        activityRepository.save(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLikedByUser(Long activityId, Long userId) {
        return activityRepository.isLikedByUser(activityId, userId);
    }

    @Override
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

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public int countActivitiesByUserId(Long userId) {
        return activityRepository.countByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Activity> searchUserActivities(Long userId, String searchText, ActivityType activityType, Pageable pageable) {
        if (activityType != null) {
            return activityRepository.searchUserActivitiesByType(userId, searchText, activityType, pageable);
        }
        return activityRepository.searchUserActivities(userId, searchText, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Activity> getActivitiesByUserIdAndType(Long userId, ActivityType activityType, Pageable pageable) {
        return activityRepository.findByUserIdAndActivityType(userId, activityType, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getUserTotalStats(Long userId) {
        int totalActivities = activityRepository.countByUserId(userId);
        Double totalDistance = activityRepository.sumDistanceByUserId(userId);
        Long totalDuration = activityRepository.sumDurationByUserId(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalActivities", totalActivities);
        stats.put("totalDistanceKm", totalDistance != null ? totalDistance : 0.0);
        stats.put("totalDurationSeconds", totalDuration != null ? totalDuration : 0L);
        return stats;
    }

    @Override
    @Transactional
    public List<String> uploadPhotos(Long activityId, Long userId, List<MultipartFile> files) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (!activity.getUser().getId().equals(userId)) {
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

    @Override
    @Transactional
    public void deletePhoto(Long activityId, Long userId, int photoIndex) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (!activity.getUser().getId().equals(userId)) {
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

    /**
     * Look up the gear by id, confirm it belongs to the caller, and confirm
     * it's allowed for this activity type. Returns null if gearId is null.
     *
     * We use GearService.getGearById so the ownership check lives in one
     * place (throws 403 if foreign, 404 if missing) and stays consistent
     * with direct gear CRUD.
     */
    private Gear resolveGear(Long gearId, Long userId, ActivityType activityType) {
        if (gearId == null) return null;
        Gear gear = gearService.getGearById(gearId, userId);
        if (gear.getActivityTypes() != null && !gear.getActivityTypes().isEmpty()
                && !gear.getActivityTypes().contains(activityType)) {
            throw new ApiRequestException(GEAR_ACTIVITY_TYPE_MISMATCH, HttpStatus.BAD_REQUEST);
        }
        return gear;
    }

    /**
     * Bookkeeping for the four possible gear transitions on update:
     *   1. no gear before or after   → no-op
     *   2. gear added                → +distance on new gear
     *   3. gear removed              → -oldDistance on old gear
     *   4. gear changed or distance changed → full reverse + re-apply
     */
    private void adjustGearMileageOnUpdate(Gear oldGear, Double oldDistance, Gear newGear, Double newDistance) {
        Long oldId = oldGear == null ? null : oldGear.getId();
        Long newId = newGear == null ? null : newGear.getId();
        boolean sameGear = oldId != null && oldId.equals(newId);

        if (sameGear) {
            // Same gear on both sides — only adjust if distance changed.
            double diff = (newDistance == null ? 0.0 : newDistance) - (oldDistance == null ? 0.0 : oldDistance);
            if (diff > 0) {
                // Direct positive adjustment without bumping activity count
                // would require a new service method; simplest correct approach
                // is to reverse then re-apply, keeping activity_count stable
                // net-net.
                gearService.decrementGearMileage(oldId, oldDistance == null ? 0.0 : oldDistance);
                gearService.incrementGearMileage(newId, newDistance == null ? 0.0 : newDistance);
            } else if (diff < 0) {
                gearService.decrementGearMileage(oldId, oldDistance == null ? 0.0 : oldDistance);
                gearService.incrementGearMileage(newId, newDistance == null ? 0.0 : newDistance);
            }
            return;
        }

        if (oldId != null && oldDistance != null) {
            gearService.decrementGearMileage(oldId, oldDistance);
        }
        if (newId != null && newDistance != null) {
            gearService.incrementGearMileage(newId, newDistance);
        }
    }

    private String calculateTrend(Double current, Double previous) {
        if (previous == null || previous == 0) {
            return current > 0 ? "+100%" : "0%";
        }
        double percentage = ((current - previous) / previous) * 100;
        return String.format("%s%.1f%%", percentage >= 0 ? "+" : "", percentage);
    }
}
