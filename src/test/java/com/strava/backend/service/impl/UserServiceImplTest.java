package com.strava.backend.service.impl;

import com.strava.backend.domain.User;
import com.strava.backend.dto.user.UpdateUserRequest;
import com.strava.backend.dto.user.UserProfileResponse;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.NotificationService;
import com.strava.backend.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static com.strava.backend.constants.ErrorMessage.USER_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private CommonMapper commonMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createUser(1L, "john@example.com", "John", "Doe");
        anotherUser = TestDataFactory.createUser(2L, "jane@example.com", "Jane", "Smith");
    }

    @Nested
    @DisplayName("getUserById Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return user when found")
        void shouldReturnUserWhenFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            User result = userService.getUserById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("john@example.com");
            assertThat(result.getFirstName()).isEqualTo("John");
            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(999L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(USER_NOT_FOUND)
                    .extracting("status")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getUserByEmail Tests")
    class GetUserByEmailTests {

        @Test
        @DisplayName("Should return user when found by email")
        void shouldReturnUserWhenFoundByEmail() {
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

            User result = userService.getUserByEmail("john@example.com");

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("john@example.com");
            verify(userRepository).findByEmail("john@example.com");
        }

        @Test
        @DisplayName("Should throw exception when email not found")
        void shouldThrowExceptionWhenEmailNotFound() {
            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserByEmail("notfound@example.com"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getAllUsers Tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return paginated users")
        void shouldReturnPaginatedUsers() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> page = new PageImpl<>(List.of(testUser, anotherUser));
            when(userRepository.findAll(pageable)).thenReturn(page);

            Page<User> result = userService.getAllUsers(pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).contains(testUser, anotherUser);
            verify(userRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Should return empty page when no users")
        void shouldReturnEmptyPageWhenNoUsers() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> emptyPage = Page.empty();
            when(userRepository.findAll(pageable)).thenReturn(emptyPage);

            Page<User> result = userService.getAllUsers(pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateUser Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user successfully")
        void shouldUpdateUserSuccessfully() {
            UpdateUserRequest request = TestDataFactory.createUpdateUserRequest();
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.updateUser(1L, request);

            assertThat(result.getFirstName()).isEqualTo("Jane");
            assertThat(result.getLastName()).isEqualTo("Smith");
            assertThat(result.getBio()).isEqualTo("Updated bio");
            assertThat(result.getCity()).isEqualTo("Krakow");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should update user weight and height")
        void shouldUpdateUserWeightAndHeight() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setFirstName("John");
            request.setLastName("Doe");
            request.setWeight(75.5);
            request.setHeight(180.0);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.updateUser(1L, request);

            assertThat(result.getWeight()).isEqualTo(75.5);
            assertThat(result.getHeight()).isEqualTo(180.0);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should update user with null weight and height")
        void shouldUpdateUserWithNullWeightAndHeight() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setFirstName("John");
            request.setLastName("Doe");
            request.setWeight(null);
            request.setHeight(null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.updateUser(1L, request);

            assertThat(result.getWeight()).isNull();
            assertThat(result.getHeight()).isNull();
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            UpdateUserRequest request = TestDataFactory.createUpdateUserRequest();
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(999L, request))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(USER_NOT_FOUND);

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("followUser Tests")
    class FollowUserTests {

        @Test
        @DisplayName("Should follow user successfully")
        void shouldFollowUserSuccessfully() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.findById(2L)).thenReturn(Optional.of(anotherUser));
            when(userRepository.save(any(User.class))).thenReturn(anotherUser);

            userService.followUser(1L, 2L);

            assertThat(anotherUser.getFollowers()).contains(testUser);
            verify(userRepository).save(anotherUser);
        }

        @Test
        @DisplayName("Should throw exception when following self")
        void shouldThrowExceptionWhenFollowingSelf() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> userService.followUser(1L, 1L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Cannot follow yourself")
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should throw exception when already following")
        void shouldThrowExceptionWhenAlreadyFollowing() {
            anotherUser.getFollowers().add(testUser);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.findById(2L)).thenReturn(Optional.of(anotherUser));

            assertThatThrownBy(() -> userService.followUser(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Already following this user");
        }
    }

    @Nested
    @DisplayName("unfollowUser Tests")
    class UnfollowUserTests {

        @Test
        @DisplayName("Should unfollow user successfully")
        void shouldUnfollowUserSuccessfully() {
            anotherUser.getFollowers().add(testUser);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.findById(2L)).thenReturn(Optional.of(anotherUser));
            when(userRepository.save(any(User.class))).thenReturn(anotherUser);

            userService.unfollowUser(1L, 2L);

            assertThat(anotherUser.getFollowers()).doesNotContain(testUser);
            verify(userRepository).save(anotherUser);
        }

        @Test
        @DisplayName("Should throw exception when not following")
        void shouldThrowExceptionWhenNotFollowing() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.findById(2L)).thenReturn(Optional.of(anotherUser));

            assertThatThrownBy(() -> userService.unfollowUser(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Not following this user");
        }
    }

    @Nested
    @DisplayName("getFollowers Tests")
    class GetFollowersTests {

        @Test
        @DisplayName("Should return followers list")
        void shouldReturnFollowersList() {
            testUser.getFollowers().add(anotherUser);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            List<User> result = userService.getFollowers(1L);

            assertThat(result).hasSize(1);
            assertThat(result).contains(anotherUser);
        }

        @Test
        @DisplayName("Should return empty list when no followers")
        void shouldReturnEmptyListWhenNoFollowers() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            List<User> result = userService.getFollowers(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getFollowing Tests")
    class GetFollowingTests {

        @Test
        @DisplayName("Should return following list")
        void shouldReturnFollowingList() {
            testUser.getFollowing().add(anotherUser);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            List<User> result = userService.getFollowing(1L);

            assertThat(result).hasSize(1);
            assertThat(result).contains(anotherUser);
        }
    }

    @Nested
    @DisplayName("isFollowing Tests")
    class IsFollowingTests {

        @Test
        @DisplayName("Should return true when following")
        void shouldReturnTrueWhenFollowing() {
            when(userRepository.isFollowing(1L, 2L)).thenReturn(true);

            boolean result = userService.isFollowing(1L, 2L);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when not following")
        void shouldReturnFalseWhenNotFollowing() {
            when(userRepository.isFollowing(1L, 2L)).thenReturn(false);

            boolean result = userService.isFollowing(1L, 2L);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getSuggestedUsers Tests")
    class GetSuggestedUsersTests {

        @Test
        @DisplayName("Should return suggested users")
        void shouldReturnSuggestedUsers() {
            User suggested1 = TestDataFactory.createUser(3L, "bob@example.com", "Bob", "Brown");
            User suggested2 = TestDataFactory.createUser(4L, "alice@example.com", "Alice", "Johnson");

            when(userRepository.findSuggestedUsers(eq(1L), any(Pageable.class)))
                    .thenReturn(List.of(suggested1, suggested2));

            List<User> result = userService.getSuggestedUsers(1L, 5);

            assertThat(result).hasSize(2);
            assertThat(result).contains(suggested1, suggested2);
            verify(userRepository).findSuggestedUsers(eq(1L), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty list when no suggestions available")
        void shouldReturnEmptyListWhenNoSuggestions() {
            when(userRepository.findSuggestedUsers(eq(1L), any(Pageable.class)))
                    .thenReturn(List.of());

            List<User> result = userService.getSuggestedUsers(1L, 5);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should respect limit parameter")
        void shouldRespectLimitParameter() {
            User suggested = TestDataFactory.createUser(3L, "bob@example.com", "Bob", "Brown");

            when(userRepository.findSuggestedUsers(eq(1L), any(Pageable.class)))
                    .thenReturn(List.of(suggested));

            List<User> result = userService.getSuggestedUsers(1L, 1);

            assertThat(result).hasSize(1);
            verify(userRepository).findSuggestedUsers(eq(1L), argThat(pageable ->
                    pageable.getPageSize() == 1 && pageable.getPageNumber() == 0));
        }
    }

    @Nested
    @DisplayName("searchUsers Privacy Tests")
    class SearchUsersPrivacyTests {

        @Test
        @DisplayName("Should delegate to repository query that excludes hideFromSearch users")
        void shouldDelegateToFilteredRepositoryQuery() {
            // Simulate the repository already excluding hidden users.
            User visible = TestDataFactory.createUser(10L, "visible@example.com", "Visible", "User");
            User hidden = TestDataFactory.createUser(11L, "hidden@example.com", "Hidden", "User");
            hidden.setHideFromSearch(true);
            Pageable pageable = PageRequest.of(0, 10);
            when(userRepository.searchUsersByText(eq("User"), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(visible)));

            Page<User> result = userService.searchUsers("User", pageable);

            assertThat(result.getContent()).containsExactly(visible);
            assertThat(result.getContent()).doesNotContain(hidden);
            verify(userRepository).searchUsersByText("User", pageable);
        }
    }

    @Nested
    @DisplayName("updateUser Privacy Tests")
    class UpdateUserPrivacyTests {

        @Test
        @DisplayName("Should update privacy flags when present in request")
        void shouldUpdatePrivacyFlags() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setFirstName("John");
            request.setLastName("Doe");
            request.setIsPrivate(true);
            request.setHideFromSearch(true);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUser(1L, request);

            assertThat(result.getIsPrivate()).isTrue();
            assertThat(result.getHideFromSearch()).isTrue();
        }

        @Test
        @DisplayName("Should leave privacy flags unchanged when absent from request")
        void shouldLeavePrivacyFlagsUnchangedWhenAbsent() {
            testUser.setIsPrivate(true);
            testUser.setHideFromSearch(true);

            UpdateUserRequest request = new UpdateUserRequest();
            request.setFirstName("John");
            request.setLastName("Doe");
            // isPrivate and hideFromSearch deliberately null

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUser(1L, request);

            assertThat(result.getIsPrivate()).isTrue();
            assertThat(result.getHideFromSearch()).isTrue();
        }
    }

    @Nested
    @DisplayName("getUserProfile Privacy Tests")
    class GetUserProfilePrivacyTests {

        @Test
        @DisplayName("Should return sanitized profile for non-follower viewing private user")
        void shouldReturnSanitizedProfileForNonFollower() {
            anotherUser.setIsPrivate(true);
            when(userRepository.findById(2L)).thenReturn(Optional.of(anotherUser));
            when(userRepository.isFollowing(2L, 1L)).thenReturn(false);

            UserProfileResponse result = userService.getUserProfile(2L, 1L);

            assertThat(result.isPrivate()).isTrue();
            assertThat(result.isCanViewActivities()).isFalse();
            assertThat(result.getFirstName()).isEqualTo("Jane");
            assertThat(result.getLastName()).isEqualTo("Smith");
            assertThat(result.getBio()).isNull();
            assertThat(result.getRecentActivities()).isEmpty();
            verifyNoInteractions(activityRepository);
        }

        @Test
        @DisplayName("Should return full profile for follower viewing private user")
        void shouldReturnFullProfileForFollower() {
            anotherUser.setIsPrivate(true);
            when(userRepository.findById(2L)).thenReturn(Optional.of(anotherUser));
            when(userRepository.isFollowing(2L, 1L)).thenReturn(true);
            when(userRepository.countFollowers(2L)).thenReturn(3);
            when(userRepository.countFollowing(2L)).thenReturn(5);
            when(activityRepository.countByUserId(2L)).thenReturn(7);
            when(activityRepository.sumDistanceByUserId(2L)).thenReturn(42.0);
            when(activityRepository.findRecentPublicByUserId(eq(2L), any(Pageable.class)))
                    .thenReturn(List.of());

            UserProfileResponse result = userService.getUserProfile(2L, 1L);

            assertThat(result.isPrivate()).isTrue();
            assertThat(result.isCanViewActivities()).isTrue();
            assertThat(result.isFollowedByCurrentUser()).isTrue();
            assertThat(result.getBio()).isEqualTo("Test bio");
            assertThat(result.getTotalActivities()).isEqualTo(7);
            assertThat(result.getTotalDistance()).isEqualTo(42.0);
        }

        @Test
        @DisplayName("Should return full profile for user viewing their own private profile")
        void shouldReturnFullProfileForSelf() {
            testUser.setIsPrivate(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.countFollowers(1L)).thenReturn(0);
            when(userRepository.countFollowing(1L)).thenReturn(0);
            when(activityRepository.countByUserId(1L)).thenReturn(2);
            when(activityRepository.sumDistanceByUserId(1L)).thenReturn(10.0);
            when(activityRepository.findRecentPublicByUserId(eq(1L), any(Pageable.class)))
                    .thenReturn(List.of());

            UserProfileResponse result = userService.getUserProfile(1L, 1L);

            assertThat(result.isPrivate()).isTrue();
            assertThat(result.isCanViewActivities()).isTrue();
            assertThat(result.isFollowedByCurrentUser()).isFalse();
            assertThat(result.getBio()).isEqualTo("Test bio");
            verify(userRepository, never()).isFollowing(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Should return full profile when target is not private")
        void shouldReturnFullProfileWhenNotPrivate() {
            anotherUser.setIsPrivate(false);
            when(userRepository.findById(2L)).thenReturn(Optional.of(anotherUser));
            when(userRepository.isFollowing(2L, 1L)).thenReturn(false);
            when(userRepository.countFollowers(2L)).thenReturn(0);
            when(userRepository.countFollowing(2L)).thenReturn(0);
            when(activityRepository.countByUserId(2L)).thenReturn(0);
            when(activityRepository.sumDistanceByUserId(2L)).thenReturn(null);
            when(activityRepository.findRecentPublicByUserId(eq(2L), any(Pageable.class)))
                    .thenReturn(List.of());

            UserProfileResponse result = userService.getUserProfile(2L, 1L);

            assertThat(result.isPrivate()).isFalse();
            assertThat(result.isCanViewActivities()).isTrue();
            assertThat(result.getBio()).isEqualTo("Test bio");
            assertThat(result.getTotalDistance()).isEqualTo(0.0);
        }
    }
}
