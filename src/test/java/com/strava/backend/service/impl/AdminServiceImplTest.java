package com.strava.backend.service.impl;

import com.strava.backend.domain.User;
import com.strava.backend.dto.admin.AdminStatsResponse;
import com.strava.backend.enums.Role;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.*;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.strava.backend.constants.ErrorMessage.USER_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminServiceImpl Unit Tests")
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createUser(1L, "john@example.com", "John", "Doe");
    }

    @Nested
    @DisplayName("getAdminStats Tests")
    class GetAdminStatsTests {

        @Test
        @DisplayName("Should return complete admin statistics")
        void shouldReturnCompleteAdminStats() {
            // Given
            given(userRepository.count()).willReturn(1000L);
            given(userRepository.countByActive(true)).willReturn(950L);
            given(activityRepository.count()).willReturn(5000L);
            given(clubRepository.count()).willReturn(50L);
            given(challengeRepository.count()).willReturn(25L);
            given(eventRepository.count()).willReturn(100L);
            given(userRepository.countUsersCreatedSince(any(LocalDateTime.class))).willReturn(50L);
            given(activityRepository.countActivitiesCreatedSince(any(LocalDateTime.class))).willReturn(200L);

            // When
            AdminStatsResponse result = adminService.getAdminStats();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalUsers()).isEqualTo(1000L);
            assertThat(result.getActiveUsers()).isEqualTo(950L);
            assertThat(result.getTotalActivities()).isEqualTo(5000L);
            assertThat(result.getTotalClubs()).isEqualTo(50L);
            assertThat(result.getTotalChallenges()).isEqualTo(25L);
            assertThat(result.getTotalEvents()).isEqualTo(100L);
            assertThat(result.getNewUsersThisWeek()).isEqualTo(50L);
            assertThat(result.getActivitiesThisWeek()).isEqualTo(200L);
        }

        @Test
        @DisplayName("Should return zero values when no data")
        void shouldReturnZeroValuesWhenNoData() {
            // Given
            given(userRepository.count()).willReturn(0L);
            given(userRepository.countByActive(true)).willReturn(0L);
            given(activityRepository.count()).willReturn(0L);
            given(clubRepository.count()).willReturn(0L);
            given(challengeRepository.count()).willReturn(0L);
            given(eventRepository.count()).willReturn(0L);
            given(userRepository.countUsersCreatedSince(any(LocalDateTime.class))).willReturn(0L);
            given(activityRepository.countActivitiesCreatedSince(any(LocalDateTime.class))).willReturn(0L);

            // When
            AdminStatsResponse result = adminService.getAdminStats();

            // Then
            assertThat(result.getTotalUsers()).isZero();
            assertThat(result.getActiveUsers()).isZero();
            assertThat(result.getTotalActivities()).isZero();
        }
    }

    @Nested
    @DisplayName("getAllUsers Tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return paginated users")
        void shouldReturnPaginatedUsers() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            User user2 = TestDataFactory.createUser(2L, "jane@example.com", "Jane", "Doe");
            Page<User> userPage = new PageImpl<>(List.of(testUser, user2));
            given(userRepository.findAllByOrderByCreatedAtDesc(pageable)).willReturn(userPage);

            // When
            Page<User> result = adminService.getAllUsers(pageable);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return empty page when no users")
        void shouldReturnEmptyPageWhenNoUsers() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> emptyPage = new PageImpl<>(List.of());
            given(userRepository.findAllByOrderByCreatedAtDesc(pageable)).willReturn(emptyPage);

            // When
            Page<User> result = adminService.getAllUsers(pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUserById Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return user when found")
        void shouldReturnUserWhenFound() {
            // Given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // When
            User result = adminService.getUserById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> adminService.getUserById(999L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(USER_NOT_FOUND)
                    .extracting("status")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("updateUserRole Tests")
    class UpdateUserRoleTests {

        @Test
        @DisplayName("Should update user role to ADMIN")
        void shouldUpdateUserRoleToAdmin() {
            // Given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            adminService.updateUserRole(1L, Role.ADMIN);

            // Then
            assertThat(testUser.getRole()).isEqualTo(Role.ADMIN);
            then(userRepository).should().save(testUser);
        }

        @Test
        @DisplayName("Should update user role to USER")
        void shouldUpdateUserRoleToUser() {
            // Given
            testUser.setRole(Role.ADMIN);
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            adminService.updateUserRole(1L, Role.USER);

            // Then
            assertThat(testUser.getRole()).isEqualTo(Role.USER);
            then(userRepository).should().save(testUser);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> adminService.updateUserRole(999L, Role.ADMIN))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(USER_NOT_FOUND);

            then(userRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("toggleUserActive Tests")
    class ToggleUserActiveTests {

        @Test
        @DisplayName("Should deactivate active user")
        void shouldDeactivateActiveUser() {
            // Given
            testUser.setActive(true);
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            adminService.toggleUserActive(1L);

            // Then
            assertThat(testUser.isActive()).isFalse();
            then(userRepository).should().save(testUser);
        }

        @Test
        @DisplayName("Should activate inactive user")
        void shouldActivateInactiveUser() {
            // Given
            testUser.setActive(false);
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            adminService.toggleUserActive(1L);

            // Then
            assertThat(testUser.isActive()).isTrue();
            then(userRepository).should().save(testUser);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> adminService.toggleUserActive(999L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(USER_NOT_FOUND);

            then(userRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteUser Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user successfully")
        void shouldDeleteUserSuccessfully() {
            // Given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // When
            adminService.deleteUser(1L);

            // Then
            then(userRepository).should().delete(testUser);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> adminService.deleteUser(999L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(USER_NOT_FOUND);

            then(userRepository).should(never()).delete(any());
        }
    }
}
