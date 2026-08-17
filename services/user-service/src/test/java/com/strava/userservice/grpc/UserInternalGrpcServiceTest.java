package com.strava.userservice.grpc;

import com.strava.grpc.user.GetNotificationProfileRequest;
import com.strava.grpc.user.GetUserSummariesRequest;
import com.strava.grpc.user.GetUserSummariesResponse;
import com.strava.grpc.user.GetVisibilityRequest;
import com.strava.grpc.user.GetVisibilityResponse;
import com.strava.grpc.user.ListWeeklySummaryRecipientsRequest;
import com.strava.grpc.user.ListWeeklySummaryRecipientsResponse;
import com.strava.grpc.user.NotificationProfile;
import com.strava.grpc.user.UserSummary;
import com.strava.userservice.domain.AuthProvider;
import com.strava.userservice.domain.Role;
import com.strava.userservice.domain.User;
import com.strava.userservice.domain.UserFollowRepository;
import com.strava.userservice.domain.UserRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Contract tests for the gRPC twin of the cluster-only {@code /internal/users} REST API.
 * Expectations are ported from {@code InternalUserControllerTest} — the gRPC responses
 * must mirror the REST semantics exactly, so treat failures here as breaking changes
 * for the downstream services, not test rot.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserInternalGrpcService")
class UserInternalGrpcServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserFollowRepository followRepository;

    @InjectMocks
    private UserInternalGrpcService grpcService;

    private User user(long id, String firstName, String lastName) {
        return User.builder()
                .id(id).firstName(firstName).lastName(lastName)
                .email(firstName.toLowerCase() + "@example.com")
                .avatar("http://avatar/" + id)
                .authProvider(AuthProvider.LOCAL).role(Role.USER).active(true)
                .build();
    }

    /** Records everything the service emits so assertions can inspect it. */
    private static final class CapturingObserver<T> implements StreamObserver<T> {
        private final List<T> values = new ArrayList<>();
        private Throwable error;
        private boolean completed;

        @Override
        public void onNext(T value) {
            values.add(value);
        }

        @Override
        public void onError(Throwable t) {
            error = t;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }

        T single() {
            assertThat(error).isNull();
            assertThat(completed).isTrue();
            assertThat(values).hasSize(1);
            return values.get(0);
        }

        Status status() {
            assertThat(values).isEmpty();
            assertThat(completed).isFalse();
            assertThat(error).isInstanceOf(StatusRuntimeException.class);
            return ((StatusRuntimeException) error).getStatus();
        }
    }

    @Nested
    @DisplayName("GetUserSummaries")
    class GetUserSummaries {

        private GetUserSummariesResponse call(long... ids) {
            GetUserSummariesRequest.Builder request = GetUserSummariesRequest.newBuilder();
            for (long id : ids) {
                request.addIds(id);
            }
            CapturingObserver<GetUserSummariesResponse> observer = new CapturingObserver<>();
            grpcService.getUserSummaries(request.build(), observer);
            return observer.single();
        }

        @Test
        void returnsExactSummaryShape() {
            given(userRepository.findByIdIn(List.of(1L, 2L))).willReturn(List.of(
                    user(1L, "John", "Doe"), user(2L, "Jane", "Smith")));

            GetUserSummariesResponse response = call(1L, 2L);

            assertThat(response.getUsersCount()).isEqualTo(2);
            UserSummary first = response.getUsers(0);
            assertThat(first.getId()).isEqualTo(1L);
            assertThat(first.getFirstName()).isEqualTo("John");
            assertThat(first.getLastName()).isEqualTo("Doe");
            assertThat(first.getFullName()).isEqualTo("John Doe");
            assertThat(first.getAvatar()).isEqualTo("http://avatar/1");
            UserSummary second = response.getUsers(1);
            assertThat(second.getId()).isEqualTo(2L);
            assertThat(second.getFullName()).isEqualTo("Jane Smith");
        }

        @Test
        void fullNameMatchesEntityDerivation() {
            User entity = user(7L, "Ada", "Lovelace");
            given(userRepository.findByIdIn(List.of(7L))).willReturn(List.of(entity));

            assertThat(call(7L).getUsers(0).getFullName())
                    .isEqualTo(entity.getFullName())
                    .isEqualTo("Ada Lovelace");
        }

        @Test
        void unknownIdsAreOmitted() {
            given(userRepository.findByIdIn(List.of(1L, 99L)))
                    .willReturn(List.of(user(1L, "John", "Doe")));

            GetUserSummariesResponse response = call(1L, 99L);

            assertThat(response.getUsersCount()).isEqualTo(1);
            assertThat(response.getUsers(0).getId()).isEqualTo(1L);
        }

        @Test
        void emptyIdListReturnsEmptyWithoutQuerying() {
            assertThat(call().getUsersCount()).isZero();
            verifyNoInteractions(userRepository);
        }
    }

    @Nested
    @DisplayName("GetVisibility")
    class GetVisibility {

        private GetVisibilityResponse call(GetVisibilityRequest request) {
            CapturingObserver<GetVisibilityResponse> observer = new CapturingObserver<>();
            grpcService.getVisibility(request, observer);
            return observer.single();
        }

        @Test
        void privateProfileWithFollowingViewer() {
            User owner = user(1L, "John", "Doe");
            owner.setIsPrivate(true);
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));
            given(followRepository.existsByUserIdAndFollowerId(1L, 2L)).willReturn(true);

            GetVisibilityResponse response = call(
                    GetVisibilityRequest.newBuilder().setUserId(1L).setViewerId(2L).build());

            assertThat(response.getIsPrivate()).isTrue();
            assertThat(response.getViewerFollows()).isTrue();
        }

        @Test
        void missingViewerMeansNotFollowing() {
            given(userRepository.findById(1L)).willReturn(Optional.of(user(1L, "John", "Doe")));

            GetVisibilityResponse response = call(
                    GetVisibilityRequest.newBuilder().setUserId(1L).build());

            assertThat(response.getIsPrivate()).isFalse();
            assertThat(response.getViewerFollows()).isFalse();
            verifyNoInteractions(followRepository);
        }

        @Test
        void selfViewerReportsNoFollowEdge() {
            User owner = user(1L, "John", "Doe");
            owner.setIsPrivate(true);
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));
            given(followRepository.existsByUserIdAndFollowerId(1L, 1L)).willReturn(false);

            GetVisibilityResponse response = call(
                    GetVisibilityRequest.newBuilder().setUserId(1L).setViewerId(1L).build());

            assertThat(response.getIsPrivate()).isTrue();
            assertThat(response.getViewerFollows()).isFalse();
        }

        @Test
        void strangerViewerDoesNotFollow() {
            User owner = user(1L, "John", "Doe");
            owner.setIsPrivate(true);
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));
            given(followRepository.existsByUserIdAndFollowerId(1L, 3L)).willReturn(false);

            GetVisibilityResponse response = call(
                    GetVisibilityRequest.newBuilder().setUserId(1L).setViewerId(3L).build());

            assertThat(response.getIsPrivate()).isTrue();
            assertThat(response.getViewerFollows()).isFalse();
        }

        @Test
        void unknownUserIsNotFound() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            CapturingObserver<GetVisibilityResponse> observer = new CapturingObserver<>();
            grpcService.getVisibility(GetVisibilityRequest.newBuilder().setUserId(99L).build(), observer);

            assertThat(observer.status().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GetNotificationProfile")
    class GetNotificationProfile {

        @Test
        void returnsExactNotificationProfileShape() {
            User entity = user(1L, "John", "Doe");
            entity.setWeeklySummary(false);
            given(userRepository.findById(1L)).willReturn(Optional.of(entity));

            CapturingObserver<NotificationProfile> observer = new CapturingObserver<>();
            grpcService.getNotificationProfile(
                    GetNotificationProfileRequest.newBuilder().setUserId(1L).build(), observer);

            NotificationProfile profile = observer.single();
            assertThat(profile.getId()).isEqualTo(1L);
            assertThat(profile.getEmail()).isEqualTo("john@example.com");
            assertThat(profile.getFirstName()).isEqualTo("John");
            assertThat(profile.getLastName()).isEqualTo("Doe");
            assertThat(profile.getFullName()).isEqualTo("John Doe");
            assertThat(profile.getAvatar()).isEqualTo("http://avatar/1");
            assertThat(profile.getEmailNotifications()).isTrue();
            assertThat(profile.getPushNotifications()).isTrue();
            assertThat(profile.getWeeklySummary()).isFalse();
            assertThat(profile.getActive()).isTrue();
        }

        @Test
        void unknownUserIsNotFound() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            CapturingObserver<NotificationProfile> observer = new CapturingObserver<>();
            grpcService.getNotificationProfile(
                    GetNotificationProfileRequest.newBuilder().setUserId(99L).build(), observer);

            Status status = observer.status();
            assertThat(status.getCode()).isEqualTo(Status.Code.NOT_FOUND);
            assertThat(status.getDescription()).isEqualTo("User not found");
        }

        @Test
        void unexpectedFailureIsInternalWithoutLeakingDetails() {
            given(userRepository.findById(1L)).willThrow(new RuntimeException("boom: select * from users"));

            CapturingObserver<NotificationProfile> observer = new CapturingObserver<>();
            grpcService.getNotificationProfile(
                    GetNotificationProfileRequest.newBuilder().setUserId(1L).build(), observer);

            Status status = observer.status();
            assertThat(status.getCode()).isEqualTo(Status.Code.INTERNAL);
            assertThat(status.getDescription()).isEqualTo("Internal error");
        }
    }

    @Nested
    @DisplayName("ListWeeklySummaryRecipients")
    class ListWeeklySummaryRecipients {

        private ListWeeklySummaryRecipientsResponse call(ListWeeklySummaryRecipientsRequest request) {
            CapturingObserver<ListWeeklySummaryRecipientsResponse> observer = new CapturingObserver<>();
            grpcService.listWeeklySummaryRecipients(request, observer);
            return observer.single();
        }

        @Test
        void returnsRecipientsWithPageTotals() {
            given(userRepository.findByWeeklySummaryTrueAndActiveTrue(PageRequest.of(0, 100)))
                    .willReturn(new PageImpl<>(List.of(user(1L, "John", "Doe")),
                            PageRequest.of(0, 100), 151));

            ListWeeklySummaryRecipientsResponse response =
                    call(ListWeeklySummaryRecipientsRequest.getDefaultInstance());

            assertThat(response.getRecipientsCount()).isEqualTo(1);
            assertThat(response.getRecipients(0).getId()).isEqualTo(1L);
            assertThat(response.getRecipients(0).getWeeklySummary()).isTrue();
            assertThat(response.getTotalElements()).isEqualTo(151L);
            assertThat(response.getTotalPages()).isEqualTo(2);
        }

        @Test
        void defaultsMirrorRestDefaults() {
            // Unset proto fields read as 0 — the service maps that to page 0, size 100,
            // exactly like the REST endpoint's missing query parameters.
            given(userRepository.findByWeeklySummaryTrueAndActiveTrue(PageRequest.of(0, 100)))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

            ListWeeklySummaryRecipientsResponse response =
                    call(ListWeeklySummaryRecipientsRequest.getDefaultInstance());

            assertThat(response.getRecipientsCount()).isZero();
            assertThat(response.getTotalElements()).isZero();
            assertThat(response.getTotalPages()).isZero();
        }

        @Test
        void honorsPageAndSize() {
            given(userRepository.findByWeeklySummaryTrueAndActiveTrue(PageRequest.of(2, 50)))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(2, 50), 0));

            ListWeeklySummaryRecipientsResponse response = call(
                    ListWeeklySummaryRecipientsRequest.newBuilder().setPage(2).setSize(50).build());

            assertThat(response.getRecipientsCount()).isZero();
        }
    }
}
