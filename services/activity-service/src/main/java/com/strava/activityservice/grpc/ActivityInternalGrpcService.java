package com.strava.activityservice.grpc;

import com.strava.activityservice.internal.InternalActivityQueryService;
import com.strava.activityservice.internal.InternalActivityQueryService.UserActivitySummaryTotals;
import com.strava.activityservice.internal.InternalActivityResponse;
import com.strava.grpc.activity.ActivityInternalServiceGrpc;
import com.strava.grpc.activity.ActivityPayload;
import com.strava.grpc.activity.ActivityStatsRange;
import com.strava.grpc.activity.GetActivitiesRequest;
import com.strava.grpc.activity.GetActivitiesResponse;
import com.strava.grpc.activity.GetStatsRangeRequest;
import com.strava.grpc.activity.GetUserActivitySummaryRequest;
import com.strava.grpc.activity.UserActivitySummary;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * CLUSTER-INTERNAL gRPC surface (grpc.server.port, default 9082) — the gRPC
 * twin of the REST /internal/activities controller. The port is never routed
 * by the gateway, so this service is only reachable from sibling services
 * inside the cluster; net.devh publishes it into Eureka instance metadata as
 * {@code gRPC.port} for discovery.
 *
 * All data access goes through {@link InternalActivityQueryService} — the
 * same bean the REST internal controller uses — so both transports share one
 * source of truth. Wire mapping rules (see activity_internal.proto):
 * LocalDateTime → ISO-8601 string via toString(); nullable fields are set
 * only when non-null (proto3 optional); ActivityType → name().
 *
 * Error contract: unparseable date-times → INVALID_ARGUMENT; anything
 * unexpected → INTERNAL with a generic description (never the exception
 * message — stack details stay in the server log).
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ActivityInternalGrpcService extends ActivityInternalServiceGrpc.ActivityInternalServiceImplBase {

    private final InternalActivityQueryService internalActivityQueryService;

    /** Batch fetch by id; unknown ids are silently skipped. */
    @Override
    public void getActivities(GetActivitiesRequest request,
                              StreamObserver<GetActivitiesResponse> responseObserver) {
        try {
            GetActivitiesResponse.Builder response = GetActivitiesResponse.newBuilder();
            internalActivityQueryService.getActivities(request.getIdsList()).stream()
                    .map(ActivityInternalGrpcService::toPayload)
                    .forEach(response::addActivities);
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(internalError("GetActivities", e));
        }
    }

    /** Aggregate totals for one user within a closed date-time window. */
    @Override
    public void getStatsRange(GetStatsRangeRequest request,
                              StreamObserver<ActivityStatsRange> responseObserver) {
        try {
            LocalDateTime from = parseDateTime(request.getFrom(), "from");
            LocalDateTime to = parseDateTime(request.getTo(), "to");

            Map<String, Object> stats = internalActivityQueryService.getRangeStats(request.getUserId(), from, to);
            responseObserver.onNext(ActivityStatsRange.newBuilder()
                    .setTotalDistanceKm(((Number) stats.get("totalDistanceKm")).doubleValue())
                    .setTotalDurationSeconds(((Number) stats.get("totalDurationSeconds")).longValue())
                    .setTotalElevationGain(((Number) stats.get("totalElevationGain")).longValue())
                    .setTotalCalories(((Number) stats.get("totalCalories")).longValue())
                    .setActivityCount(((Number) stats.get("activityCount")).intValue())
                    .build());
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        } catch (Exception e) {
            responseObserver.onError(internalError("GetStatsRange", e));
        }
    }

    /** Per-user summary with optional sport filter, aggregated server-side. */
    @Override
    public void getUserActivitySummary(GetUserActivitySummaryRequest request,
                                       StreamObserver<UserActivitySummary> responseObserver) {
        try {
            LocalDateTime from = parseDateTime(request.getFrom(), "from");
            LocalDateTime to = parseDateTime(request.getTo(), "to");
            String sportFilter = request.hasSportFilter() ? request.getSportFilter() : null;

            UserActivitySummaryTotals totals =
                    internalActivityQueryService.summarize(request.getUserId(), sportFilter, from, to);
            responseObserver.onNext(UserActivitySummary.newBuilder()
                    .setTotalDistanceKm(totals.totalDistanceKm())
                    .setTotalDurationSeconds(totals.totalDurationSeconds())
                    .setTotalElevationMeters(totals.totalElevationMeters())
                    .setActivityCount(totals.activityCount())
                    .build());
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        } catch (Exception e) {
            responseObserver.onError(internalError("GetUserActivitySummary", e));
        }
    }

    /**
     * Maps the shared internal DTO onto the wire payload. proto3 optional
     * fields are set only when the source value is non-null, so callers can
     * distinguish "absent" from a zero default; repeated fields stay empty
     * when the parsed stream arrays are null.
     */
    private static ActivityPayload toPayload(InternalActivityResponse a) {
        ActivityPayload.Builder b = ActivityPayload.newBuilder()
                .setId(a.id())
                .setUserId(a.userId())
                .setTitle(a.title())
                .setActivityType(a.activityType().name())
                .setIsPrivate(a.isPrivate());
        if (a.description() != null) b.setDescription(a.description());
        if (a.distance() != null) b.setDistance(a.distance());
        if (a.duration() != null) b.setDuration(a.duration());
        if (a.elevationGain() != null) b.setElevationGain(a.elevationGain());
        if (a.calories() != null) b.setCalories(a.calories());
        if (a.averageSpeed() != null) b.setAverageSpeed(a.averageSpeed());
        if (a.maxSpeed() != null) b.setMaxSpeed(a.maxSpeed());
        if (a.averageHeartRate() != null) b.setAverageHeartRate(a.averageHeartRate());
        if (a.maxHeartRate() != null) b.setMaxHeartRate(a.maxHeartRate());
        if (a.averageCadence() != null) b.setAverageCadence(a.averageCadence());
        if (a.averagePower() != null) b.setAveragePower(a.averagePower());
        if (a.startTime() != null) b.setStartTime(a.startTime().toString());
        if (a.endTime() != null) b.setEndTime(a.endTime().toString());
        if (a.startLocation() != null) b.setStartLocation(a.startLocation());
        if (a.endLocation() != null) b.setEndLocation(a.endLocation());
        if (a.routePolyline() != null) b.setRoutePolyline(a.routePolyline());
        if (a.elevationProfile() != null) {
            for (double v : a.elevationProfile()) b.addElevationProfile(v);
        }
        if (a.pointTimestamps() != null) {
            for (long v : a.pointTimestamps()) b.addPointTimestamps(v);
        }
        if (a.mapImageUrl() != null) b.setMapImageUrl(a.mapImageUrl());
        if (a.photos() != null) b.addAllPhotos(a.photos());
        if (a.createdAt() != null) b.setCreatedAt(a.createdAt().toString());
        if (a.updatedAt() != null) b.setUpdatedAt(a.updatedAt().toString());
        return b.build();
    }

    private static LocalDateTime parseDateTime(String value, String field) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw Status.INVALID_ARGUMENT
                    .withDescription("Invalid ISO-8601 local date-time for '" + field + "': " + value)
                    .asRuntimeException();
        }
    }

    private static StatusRuntimeException internalError(String rpc, Exception e) {
        // Full details go to the server log only — the status description must
        // never leak exception messages or stack traces across the wire.
        log.error("{} failed", rpc, e);
        return Status.INTERNAL.withDescription("Internal error").asRuntimeException();
    }
}
