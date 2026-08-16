package com.strava.activityservice.activity;

import com.strava.activityservice.client.SocialSummaryClient;
import com.strava.activityservice.client.SocialSummaryClient.SocialSummary;
import com.strava.activityservice.client.UserVisibilityClient;
import com.strava.activityservice.common.HeaderResponse;
import com.strava.activityservice.domain.Activity;
import com.strava.activityservice.domain.ActivityType;
import com.strava.activityservice.exception.ApiRequestException;
import com.strava.activityservice.gear.GearMapper;
import com.strava.activityservice.user.BaseUserResponse;
import com.strava.activityservice.user.UserCacheService;
import com.strava.activityservice.util.StreamJson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.strava.activityservice.constants.ErrorMessage.ACTIVITY_NOT_FOUND;

/**
 * Response assembly + privacy envelope, ported from main's ActivityMapper.
 * Cross-service data is composed here: social counts arrive in one batched
 * call per page (SocialSummaryClient) and the author objects come from the
 * local user_cache projection — never a per-row remote call.
 *
 * The public entry points are transactional: this service runs with
 * open-in-view=false (unlike the monolith's OSIV), so the lazy photos
 * collection must be initialized while a session is still open. The DTOs get
 * a detached copy of the list, never the live Hibernate collection.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityMapper {

    private final ActivityVisibility activityVisibility;
    private final ActivityService activityService;
    private final UserVisibilityClient userVisibilityClient;
    private final SocialSummaryClient socialSummaryClient;
    private final UserCacheService userCacheService;
    private final GearMapper gearMapper;

    @Transactional(readOnly = true)
    public FullActivityResponse getActivityById(Long activityId, Long currentUserId) {
        Activity activity = activityService.getActivityById(activityId);
        // Same 404-on-deny main uses, so ids cannot be probed.
        if (!activityVisibility.isViewableBy(currentUserId, activity)) {
            throw new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        return mapToFullActivityResponse(activity, currentUserId);
    }

    @Transactional(readOnly = true)
    public HeaderResponse<ActivityResponse> getActivitiesByUserId(Long userId, Long currentUserId, Pageable pageable) {
        // Respect private-profile flag: non-owner, non-follower viewers see nothing.
        // (Mirrors main: activity-level isPrivate is NOT filtered on a visible
        // profile — the owner's page shows the same list the owner sees.)
        if (currentUserId == null || !currentUserId.equals(userId)) {
            UserVisibilityClient.VisibilityInfo target = userVisibilityClient.visibilityFor(userId, currentUserId);
            if (target.isPrivate() && !target.viewerFollows()) {
                return HeaderResponse.of(List.of(), 0, 0L);
            }
        }
        Page<Activity> activities = activityService.getActivitiesByUserId(userId, pageable);
        List<ActivityResponse> responses = mapPage(activities.getContent(), currentUserId);
        return HeaderResponse.of(responses, activities.getTotalPages(), activities.getTotalElements());
    }

    @Transactional
    public ActivityResponse createActivity(Long userId, ActivityRequest request) {
        Activity activity = activityService.createActivity(userId, request);
        return mapToActivityResponse(activity, userId);
    }

    @Transactional
    public ActivityResponse updateActivity(Long activityId, Long userId, ActivityRequest request) {
        Activity activity = activityService.updateActivity(activityId, userId, request);
        return mapToActivityResponse(activity, userId);
    }

    public void deleteActivity(Long activityId, Long userId) {
        activityService.deleteActivity(activityId, userId);
    }

    public ActivityStatsResponse getMonthlyStats(Long userId) {
        return activityService.getMonthlyStats(userId);
    }

    public WeeklyStatsResponse getWeeklyStats(Long userId) {
        return activityService.getWeeklyStats(userId);
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> searchMyActivities(Long userId, String searchText, ActivityType activityType, Pageable pageable) {
        Page<Activity> activities = activityService.searchUserActivities(userId, searchText, activityType, pageable);
        return mapPage(activities.getContent(), userId);
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> getActivitiesByType(Long userId, ActivityType activityType, Pageable pageable) {
        Page<Activity> activities = activityService.getActivitiesByUserIdAndType(userId, activityType, pageable);
        return mapPage(activities.getContent(), userId);
    }

    public Map<String, Object> getUserTotalStats(Long userId) {
        return activityService.getUserTotalStats(userId);
    }

    private List<ActivityResponse> mapPage(List<Activity> activities, Long currentUserId) {
        List<Long> ids = activities.stream().map(Activity::getId).toList();
        List<Long> userIds = activities.stream().map(Activity::getUserId).toList();
        Map<Long, SocialSummary> social = socialSummaryClient.summariesFor(ids, currentUserId);
        Map<Long, BaseUserResponse> users = userCacheService.usersFor(userIds);
        return activities.stream()
                .map(activity -> mapToActivityResponse(
                        activity,
                        social.getOrDefault(activity.getId(), SocialSummary.empty(activity.getId())),
                        users.get(activity.getUserId())))
                .toList();
    }

    /**
     * Public so the import + Strava flows (which persist through the same
     * create pipeline) can reuse the exact response assembly.
     */
    @Transactional(readOnly = true)
    public ActivityResponse mapToActivityResponse(Activity activity, Long currentUserId) {
        SocialSummary social = socialSummaryClient.summariesFor(List.of(activity.getId()), currentUserId)
                .getOrDefault(activity.getId(), SocialSummary.empty(activity.getId()));
        return mapToActivityResponse(activity, social, userCacheService.userFor(activity.getUserId()));
    }

    private ActivityResponse mapToActivityResponse(Activity activity, SocialSummary social, BaseUserResponse user) {
        ActivityResponse response = new ActivityResponse();
        response.setId(activity.getId());
        response.setTitle(activity.getTitle());
        response.setDescription(activity.getDescription());
        response.setActivityType(activity.getActivityType());
        response.setDistance(activity.getDistance());
        response.setDuration(activity.getDuration());
        response.setElevationGain(activity.getElevationGain());
        response.setCalories(activity.getCalories());
        response.setAverageSpeed(activity.getAverageSpeed());
        response.setStartTime(activity.getStartTime());
        response.setStartLocation(activity.getStartLocation());
        response.setPrivate(activity.isPrivate());
        response.setCreatedAt(activity.getCreatedAt());
        response.setRoutePolyline(activity.getRoutePolyline());
        response.setAverageHeartRate(activity.getAverageHeartRate());
        response.setMaxHeartRate(activity.getMaxHeartRate());
        response.setPhotos(new ArrayList<>(activity.getPhotos()));
        response.setLikesCount(social.likesCount());
        response.setCommentsCount(social.commentsCount());
        response.setLikedByCurrentUser(social.likedByViewer());
        response.setUser(user);
        response.setElevationProfile(StreamJson.parseElevationProfile(activity.getElevationProfile()));
        response.setPointTimestamps(StreamJson.parsePointTimestamps(activity.getPointTimestamps()));
        response.setGear(gearMapper.toSummary(activity.getGear()));
        return response;
    }

    private FullActivityResponse mapToFullActivityResponse(Activity activity, Long currentUserId) {
        SocialSummary social = socialSummaryClient.summariesFor(List.of(activity.getId()), currentUserId)
                .getOrDefault(activity.getId(), SocialSummary.empty(activity.getId()));

        FullActivityResponse response = new FullActivityResponse();
        response.setId(activity.getId());
        response.setTitle(activity.getTitle());
        response.setDescription(activity.getDescription());
        response.setActivityType(activity.getActivityType());
        response.setDistance(activity.getDistance());
        response.setDuration(activity.getDuration());
        response.setElevationGain(activity.getElevationGain());
        response.setCalories(activity.getCalories());
        response.setAverageSpeed(activity.getAverageSpeed());
        response.setMaxSpeed(activity.getMaxSpeed());
        response.setAverageHeartRate(activity.getAverageHeartRate());
        response.setMaxHeartRate(activity.getMaxHeartRate());
        response.setAverageCadence(activity.getAverageCadence());
        response.setAveragePower(activity.getAveragePower());
        response.setStartTime(activity.getStartTime());
        response.setEndTime(activity.getEndTime());
        response.setStartLocation(activity.getStartLocation());
        response.setEndLocation(activity.getEndLocation());
        response.setRoutePolyline(activity.getRoutePolyline());
        response.setMapImageUrl(activity.getMapImageUrl());
        response.setPrivate(activity.isPrivate());
        response.setCreatedAt(activity.getCreatedAt());
        response.setUpdatedAt(activity.getUpdatedAt());
        response.setPhotos(new ArrayList<>(activity.getPhotos()));
        response.setLikesCount(social.likesCount());
        response.setCommentsCount(social.commentsCount());
        response.setLikedByCurrentUser(social.likedByViewer());
        response.setUser(userCacheService.userFor(activity.getUserId()));
        response.setElevationProfile(StreamJson.parseElevationProfile(activity.getElevationProfile()));
        response.setPointTimestamps(StreamJson.parsePointTimestamps(activity.getPointTimestamps()));
        // Comment storage is owned by social-service; the frontend loads the
        // thread from /{id}/comments, so the embedded list is always empty here.
        response.setComments(List.of());
        response.setGear(gearMapper.toSummary(activity.getGear()));
        return response;
    }

}
