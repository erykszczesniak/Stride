package com.strava.backend.service.impl;

import com.strava.backend.domain.User;
import com.strava.backend.dto.user.UpdateUserRequest;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.UserRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

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
}
