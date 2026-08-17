package com.strava.userservice.grpc;

import com.strava.grpc.user.GetNotificationProfileRequest;
import com.strava.grpc.user.GetUserSummariesRequest;
import com.strava.grpc.user.GetUserSummariesResponse;
import com.strava.grpc.user.GetVisibilityRequest;
import com.strava.grpc.user.GetVisibilityResponse;
import com.strava.grpc.user.ListWeeklySummaryRecipientsRequest;
import com.strava.grpc.user.ListWeeklySummaryRecipientsResponse;
import com.strava.grpc.user.NotificationProfile;
import com.strava.grpc.user.UserInternalServiceGrpc;
import com.strava.grpc.user.UserSummary;
import com.strava.userservice.domain.User;
import com.strava.userservice.domain.UserFollowRepository;
import com.strava.userservice.domain.UserRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.function.Supplier;

import static com.strava.userservice.common.ErrorMessages.USER_NOT_FOUND;

/**
 * gRPC twin of {@link com.strava.userservice.internal.InternalUserController}, serving
 * the {@code stride.internal.v1.UserInternalService} contract on the cluster-internal
 * gRPC port. Semantics mirror the REST {@code /internal/users} endpoints exactly:
 * unknown ids are silently skipped in the batch lookup, single-user lookups map a
 * missing user to {@link Status#NOT_FOUND}, and the weekly-summary recipient page
 * carries the same totals as the Spring {@code Page} JSON shape.
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserInternalGrpcService extends UserInternalServiceGrpc.UserInternalServiceImplBase {

    /**
     * Mirrors the REST endpoint's {@code size} default. proto3 cannot distinguish an
     * unset int from 0, so 0 (and negatives) mean "use the default", like an omitted
     * query parameter.
     */
    private static final int DEFAULT_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final UserFollowRepository followRepository;

    /** Batch summary lookup, e.g. for social-feed hydration. Unknown ids are simply omitted. */
    @Override
    public void getUserSummaries(GetUserSummariesRequest request,
                                 StreamObserver<GetUserSummariesResponse> responseObserver) {
        respond(responseObserver, () -> {
            List<Long> ids = request.getIdsList();
            GetUserSummariesResponse.Builder response = GetUserSummariesResponse.newBuilder();
            if (!ids.isEmpty()) {
                userRepository.findByIdIn(ids).forEach(user -> response.addUsers(toSummary(user)));
            }
            return response.build();
        });
    }

    /** Privacy check used by activity/social services to decide feed/profile visibility. */
    @Override
    public void getVisibility(GetVisibilityRequest request,
                              StreamObserver<GetVisibilityResponse> responseObserver) {
        respond(responseObserver, () -> {
            User user = getUser(request.getUserId());
            boolean viewerFollows = request.hasViewerId()
                    && followRepository.existsByUserIdAndFollowerId(request.getUserId(), request.getViewerId());
            return GetVisibilityResponse.newBuilder()
                    .setIsPrivate(Boolean.TRUE.equals(user.getIsPrivate()))
                    .setViewerFollows(viewerFollows)
                    .build();
        });
    }

    /** Contact + notification preferences for notification-service. */
    @Override
    public void getNotificationProfile(GetNotificationProfileRequest request,
                                       StreamObserver<NotificationProfile> responseObserver) {
        respond(responseObserver, () -> toNotificationProfile(getUser(request.getUserId())));
    }

    /** Users opted into (and eligible for) the weekly summary digest, page by page. */
    @Override
    public void listWeeklySummaryRecipients(ListWeeklySummaryRecipientsRequest request,
                                            StreamObserver<ListWeeklySummaryRecipientsResponse> responseObserver) {
        respond(responseObserver, () -> {
            int page = Math.max(request.getPage(), 0);
            int size = request.getSize() > 0 ? request.getSize() : DEFAULT_PAGE_SIZE;
            Page<User> recipients =
                    userRepository.findByWeeklySummaryTrueAndActiveTrue(PageRequest.of(page, size));
            ListWeeklySummaryRecipientsResponse.Builder response = ListWeeklySummaryRecipientsResponse.newBuilder()
                    .setTotalElements(recipients.getTotalElements())
                    .setTotalPages(recipients.getTotalPages());
            recipients.forEach(user -> response.addRecipients(toNotificationProfile(user)));
            return response.build();
        });
    }

    private User getUser(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> Status.NOT_FOUND.withDescription(USER_NOT_FOUND).asRuntimeException());
    }

    private static UserSummary toSummary(User user) {
        UserSummary.Builder summary = UserSummary.newBuilder().setId(user.getId());
        if (user.getFirstName() != null) summary.setFirstName(user.getFirstName());
        if (user.getLastName() != null) summary.setLastName(user.getLastName());
        if (user.getFullName() != null) summary.setFullName(user.getFullName());
        if (user.getAvatar() != null) summary.setAvatar(user.getAvatar());
        return summary.build();
    }

    private static NotificationProfile toNotificationProfile(User user) {
        NotificationProfile.Builder profile = NotificationProfile.newBuilder()
                .setId(user.getId())
                .setEmail(user.getEmail())
                .setEmailNotifications(user.isEmailNotifications())
                .setPushNotifications(user.isPushNotifications())
                .setWeeklySummary(user.isWeeklySummary())
                .setActive(user.isActive());
        if (user.getFirstName() != null) profile.setFirstName(user.getFirstName());
        if (user.getLastName() != null) profile.setLastName(user.getLastName());
        if (user.getFullName() != null) profile.setFullName(user.getFullName());
        if (user.getAvatar() != null) profile.setAvatar(user.getAvatar());
        return profile.build();
    }

    /**
     * Runs one unary handler: emits the produced message, passes through deliberate
     * {@link StatusRuntimeException}s (e.g. NOT_FOUND) and maps anything unexpected to
     * a description-free INTERNAL so no stack trace or SQL detail leaks to callers.
     */
    private <T> void respond(StreamObserver<T> responseObserver, Supplier<T> handler) {
        try {
            T message = handler.get();
            responseObserver.onNext(message);
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        } catch (Exception e) {
            log.error("Unexpected error handling internal gRPC call", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Internal error").asRuntimeException());
        }
    }
}
