package com.strava.userservice.user;

import com.strava.userservice.domain.AuthProvider;
import com.strava.userservice.domain.Role;
import com.strava.userservice.domain.User;
import com.strava.userservice.domain.UserFollow;
import com.strava.userservice.domain.UserFollowRepository;
import com.strava.userservice.domain.UserRepository;
import com.strava.userservice.event.UserEventPublisher;
import com.strava.userservice.exception.ApiRequestException;
import com.strava.userservice.search.UserDeletedEvent;
import com.strava.userservice.search.UserDocument;
import com.strava.userservice.search.UserSearchRepository;
import com.strava.userservice.search.UserUpsertedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.strava.userservice.common.ErrorMessages.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserFollowRepository followRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ApplicationEventPublisher localEvents;
    @Mock private UserEventPublisher kafkaEvents;
    @Mock private ActivityStatsPort activityStats;
    @Mock private ObjectProvider<UserSearchRepository> searchRepositoryProvider;
    @Mock private UserSearchRepository searchRepository;

    @InjectMocks private UserService userService;

    private User user;
    private User other;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).firstName("John").lastName("Doe").email("john@example.com")
                .password("$hashed$").authProvider(AuthProvider.LOCAL).role(Role.USER)
                .active(true).createdAt(LocalDateTime.now())
                .build();
        other = User.builder()
                .id(2L).firstName("Jane").lastName("Smith").email("jane@example.com")
                .authProvider(AuthProvider.LOCAL).role(Role.USER).active(true)
                .build();
    }

    @Nested
    @DisplayName("getUserResponse")
    class GetUserResponse {

        @Test
        void populatesCountersFromFollowGraphAndActivityPort() {
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(followRepository.countByUserId(1L)).willReturn(3L);
            given(followRepository.countByFollowerId(1L)).willReturn(2L);
            given(activityStats.countActivities(1L)).willReturn(5);

            UserResponse response = userService.getUserResponse(1L);

            assertThat(response.followersCount()).isEqualTo(3);
            assertThat(response.followingCount()).isEqualTo(2);
            assertThat(response.activitiesCount()).isEqualTo(5);
            assertThat(response.fullName()).isEqualTo("John Doe");
        }

        @Test
        void unknownUserIs404() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserResponse(99L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(USER_NOT_FOUND)
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        void blankQueryReturnsAllUsers() {
            Pageable pageable = PageRequest.of(0, 20);
            given(userRepository.findAll(pageable)).willReturn(new PageImpl<>(List.of(user)));

            assertThat(userService.searchUsers("  ", pageable).getContent()).containsExactly(user);
        }

        @Test
        void preservesElasticsearchScoreOrderWhenHydrating() {
            Pageable pageable = PageRequest.of(0, 20);
            given(searchRepositoryProvider.getIfAvailable()).willReturn(searchRepository);
            given(searchRepository.searchByQuery("jo", pageable)).willReturn(new PageImpl<>(List.of(
                    UserDocument.builder().id(2L).build(),
                    UserDocument.builder().id(1L).build())));
            // JPA returns rows in id order — the service must restore ES order.
            given(userRepository.findByIdIn(List.of(2L, 1L))).willReturn(List.of(user, other));

            List<User> result = userService.searchUsers("jo", pageable).getContent();

            assertThat(result).containsExactly(other, user);
        }

        @Test
        void throwsWhenElasticsearchUnavailableSoControllerCanFallBack() {
            given(searchRepositoryProvider.getIfAvailable()).willReturn(null);

            assertThatThrownBy(() -> userService.searchUsers("jo", PageRequest.of(0, 20)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void fallbackDelegatesToSqlSearch() {
            Pageable pageable = PageRequest.of(0, 20);
            given(userRepository.searchUsersByText("jo", pageable))
                    .willReturn(new PageImpl<>(List.of(user)));

            assertThat(userService.searchUsersFallback(" jo ", pageable).getContent())
                    .containsExactly(user);
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        void overwritesProfileFieldsAndPublishesBothEvents() {
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.save(user)).willReturn(user);

            userService.updateUser(1L, new UpdateUserRequest(
                    "Johnny", "Doe", "new bio", "Krakow", "Poland",
                    72.5, 180.0, false, null, null, true, null));

            assertThat(user.getFirstName()).isEqualTo("Johnny");
            assertThat(user.getBio()).isEqualTo("new bio");
            assertThat(user.getWeight()).isEqualTo(72.5);
            assertThat(user.isEmailNotifications()).isFalse();
            assertThat(user.isPushNotifications()).isTrue(); // null in request -> untouched
            assertThat(user.getIsPrivate()).isTrue();
            verify(localEvents).publishEvent(any(UserUpsertedEvent.class));
            verify(kafkaEvents).publishUserUpdated(user);
        }

        @Test
        void nullOptionalFieldsClearThem() {
            // Parity with the monolith: bio/city/country/weight/height overwrite unconditionally.
            user.setBio("old bio");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.save(user)).willReturn(user);

            userService.updateUser(1L, new UpdateUserRequest(
                    "John", "Doe", null, null, null, null, null, null, null, null, null, null));

            assertThat(user.getBio()).isNull();
        }
    }

    @Nested
    @DisplayName("follow / unfollow")
    class FollowUnfollow {

        @Test
        void followSavesEdgeAndPublishesFollowedEvent() {
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.findById(2L)).willReturn(Optional.of(other));
            given(followRepository.existsByUserIdAndFollowerId(2L, 1L)).willReturn(false);

            userService.followUser(1L, 2L);

            ArgumentCaptor<UserFollow> edge = ArgumentCaptor.forClass(UserFollow.class);
            verify(followRepository).save(edge.capture());
            assertThat(edge.getValue().getUserId()).isEqualTo(2L);
            assertThat(edge.getValue().getFollowerId()).isEqualTo(1L);
            verify(kafkaEvents).publishUserFollowed(user, 2L);
        }

        @Test
        void cannotFollowYourself() {
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.followUser(1L, 1L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Cannot follow yourself")
                    .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void cannotFollowTwice() {
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.findById(2L)).willReturn(Optional.of(other));
            given(followRepository.existsByUserIdAndFollowerId(2L, 1L)).willReturn(true);

            assertThatThrownBy(() -> userService.followUser(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Already following this user");
        }

        @Test
        void unfollowRemovesEdgeAndPublishesUnfollowedEvent() {
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.findById(2L)).willReturn(Optional.of(other));
            given(followRepository.existsByUserIdAndFollowerId(2L, 1L)).willReturn(true);

            userService.unfollowUser(1L, 2L);

            verify(followRepository).deleteByUserIdAndFollowerId(2L, 1L);
            verify(kafkaEvents).publishUserUnfollowed(1L, 2L);
        }

        @Test
        void unfollowWithoutFollowingIs400() {
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.findById(2L)).willReturn(Optional.of(other));
            given(followRepository.existsByUserIdAndFollowerId(2L, 1L)).willReturn(false);

            assertThatThrownBy(() -> userService.unfollowUser(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Not following this user");
        }
    }

    @Nested
    @DisplayName("getUserProfile")
    class GetUserProfile {

        @Test
        void privateProfileViewedByStrangerIsSanitized() {
            user.setIsPrivate(true);
            user.setBio("secret bio");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(followRepository.existsByUserIdAndFollowerId(1L, 2L)).willReturn(false);

            UserProfileResponse profile = userService.getUserProfile(1L, 2L);

            assertThat(profile.canViewActivities()).isFalse();
            assertThat(profile.isPrivate()).isTrue();
            assertThat(profile.firstName()).isEqualTo("John");
            assertThat(profile.bio()).isNull();
            assertThat(profile.createdAt()).isNull();
            assertThat(profile.recentActivities()).isEmpty();
            assertThat(profile.followedByCurrentUser()).isFalse();
        }

        @Test
        void privateProfileViewedByFollowerIsVisible() {
            user.setIsPrivate(true);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(followRepository.existsByUserIdAndFollowerId(1L, 2L)).willReturn(true);
            given(followRepository.countByUserId(1L)).willReturn(1L);
            given(followRepository.countByFollowerId(1L)).willReturn(0L);
            given(activityStats.totalsFor(1L)).willReturn(ActivityStatsPort.ActivityTotals.empty());
            given(activityStats.recentPublicActivities(1L, 2L)).willReturn(List.of());

            UserProfileResponse profile = userService.getUserProfile(1L, 2L);

            assertThat(profile.canViewActivities()).isTrue();
            assertThat(profile.followedByCurrentUser()).isTrue();
            assertThat(profile.isPrivate()).isTrue();
        }

        @Test
        void privateProfileViewedByOwnerIsVisible() {
            user.setIsPrivate(true);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(followRepository.countByUserId(1L)).willReturn(0L);
            given(followRepository.countByFollowerId(1L)).willReturn(0L);
            given(activityStats.totalsFor(1L)).willReturn(ActivityStatsPort.ActivityTotals.empty());
            given(activityStats.recentPublicActivities(1L, 1L)).willReturn(List.of());

            UserProfileResponse profile = userService.getUserProfile(1L, 1L);

            assertThat(profile.canViewActivities()).isTrue();
            assertThat(profile.followedByCurrentUser()).isFalse();
            verify(followRepository, never()).existsByUserIdAndFollowerId(anyLong(), anyLong());
        }

        @Test
        void publicProfileCarriesActivityTotalsAndCounts() {
            user.setBio("public bio");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(followRepository.existsByUserIdAndFollowerId(1L, 2L)).willReturn(false);
            given(followRepository.countByUserId(1L)).willReturn(4L);
            given(followRepository.countByFollowerId(1L)).willReturn(3L);
            given(activityStats.totalsFor(1L))
                    .willReturn(new ActivityStatsPort.ActivityTotals(6, 42.5, 3600L, 120L, 900L));
            given(activityStats.recentPublicActivities(1L, 2L)).willReturn(List.of());

            UserProfileResponse profile = userService.getUserProfile(1L, 2L);

            assertThat(profile.bio()).isEqualTo("public bio");
            assertThat(profile.totalActivities()).isEqualTo(6);
            assertThat(profile.totalDistance()).isEqualTo(42.5);
            assertThat(profile.followersCount()).isEqualTo(4);
            assertThat(profile.followingCount()).isEqualTo(3);
            assertThat(profile.canViewActivities()).isTrue();
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        void encodesNewPasswordWhenCurrentMatches() {
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("old", "$hashed$")).willReturn(true);
            given(passwordEncoder.encode("new-password")).willReturn("$new$");

            userService.changePassword(1L, "old", "new-password");

            assertThat(user.getPassword()).isEqualTo("$new$");
            verify(userRepository).save(user);
        }

        @Test
        void rejectsWrongCurrentPassword() {
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrong", "$hashed$")).willReturn(false);

            assertThatThrownBy(() -> userService.changePassword(1L, "wrong", "new-password"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(INCORRECT_PASSWORD);
        }

        @Test
        void rejectsOauthAccounts() {
            user.setAuthProvider(AuthProvider.GOOGLE);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.changePassword(1L, "old", "new-password"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(PASSWORD_CHANGE_NOT_ALLOWED);
        }
    }

    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccount {

        @Test
        void deletesUserAndPublishesBothDeletionEvents() {
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            userService.deleteAccount(1L);

            verify(userRepository).delete(user);
            verify(localEvents).publishEvent(any(UserDeletedEvent.class));
            verify(kafkaEvents).publishUserDeleted(1L);
        }
    }

    @Nested
    @DisplayName("followers / following / suggested")
    class Lists {

        @Test
        void followersResolvesUsersFromEdges() {
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(followRepository.findAllByUserId(1L)).willReturn(List.of(
                    UserFollow.builder().userId(1L).followerId(2L).build()));
            given(userRepository.findByIdIn(List.of(2L))).willReturn(List.of(other));

            assertThat(userService.getFollowers(1L)).containsExactly(other);
        }

        @Test
        void followingResolvesUsersFromEdges() {
            given(userRepository.findById(2L)).willReturn(Optional.of(other));
            given(followRepository.findAllByFollowerId(2L)).willReturn(List.of(
                    UserFollow.builder().userId(1L).followerId(2L).build()));
            given(userRepository.findByIdIn(List.of(1L))).willReturn(List.of(user));

            assertThat(userService.getFollowing(2L)).containsExactly(user);
        }

        @Test
        void suggestedDelegatesWithLimit() {
            given(userRepository.findSuggestedUsers(1L, PageRequest.of(0, 5)))
                    .willReturn(List.of(other));

            assertThat(userService.getSuggestedUsers(1L, 5)).containsExactly(other);
        }
    }
}
