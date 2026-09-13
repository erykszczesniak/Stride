package com.strava.backend.service.impl;

import com.strava.backend.domain.Goal;
import com.strava.backend.domain.User;
import com.strava.backend.dto.goal.GoalRequest;
import com.strava.backend.enums.GoalPeriod;
import com.strava.backend.enums.GoalType;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.GoalRepository;
import com.strava.backend.service.UserService;
import com.strava.backend.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.strava.backend.constants.ErrorMessage.GOAL_ACCESS_DENIED;
import static com.strava.backend.constants.ErrorMessage.GOAL_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoalServiceImpl Unit Tests")
class GoalServiceImplTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private GoalServiceImpl goalService;

    private User testUser;
    private User anotherUser;
    private Goal testGoal;
    private GoalRequest goalRequest;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createUser(1L, "john@example.com", "John", "Doe");
        anotherUser = TestDataFactory.createUser(2L, "jane@example.com", "Jane", "Smith");
        testGoal = TestDataFactory.createGoal(1L, testUser);
        goalRequest = TestDataFactory.createGoalRequest();
    }

    @Nested
    @DisplayName("getGoalById Tests")
    class GetGoalByIdTests {

        @Test
        @DisplayName("Should return goal when found")
        void shouldReturnGoalWhenFound() {
            when(goalRepository.findById(1L)).thenReturn(Optional.of(testGoal));

            Goal result = goalService.getGoalById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getGoalType()).isEqualTo(GoalType.DISTANCE);
            verify(goalRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw exception when goal not found")
        void shouldThrowExceptionWhenGoalNotFound() {
            when(goalRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> goalService.getGoalById(999L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(GOAL_NOT_FOUND)
                    .extracting("status")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getActiveGoalsByUserId Tests")
    class GetActiveGoalsByUserIdTests {

        @Test
        @DisplayName("Should return active goals")
        void shouldReturnActiveGoals() {
            Goal goal2 = TestDataFactory.createGoal(2L, testUser);
            when(goalRepository.findActiveGoalsByUserId(1L)).thenReturn(List.of(testGoal, goal2));

            List<Goal> result = goalService.getActiveGoalsByUserId(1L);

            assertThat(result).hasSize(2);
            verify(goalRepository).findActiveGoalsByUserId(1L);
        }

        @Test
        @DisplayName("Should return empty list when no active goals")
        void shouldReturnEmptyListWhenNoActiveGoals() {
            when(goalRepository.findActiveGoalsByUserId(1L)).thenReturn(Collections.emptyList());

            List<Goal> result = goalService.getActiveGoalsByUserId(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAllGoalsByUserId Tests")
    class GetAllGoalsByUserIdTests {

        @Test
        @DisplayName("Should return all goals ordered by created date")
        void shouldReturnAllGoalsOrderedByCreatedDate() {
            Goal goal2 = TestDataFactory.createGoal(2L, testUser);
            when(goalRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(testGoal, goal2));

            List<Goal> result = goalService.getAllGoalsByUserId(1L);

            assertThat(result).hasSize(2);
            verify(goalRepository).findByUserIdOrderByCreatedAtDesc(1L);
        }
    }

    @Nested
    @DisplayName("getActiveGoalByPeriod Tests")
    class GetActiveGoalByPeriodTests {

        @Test
        @DisplayName("Should return active goal for period")
        void shouldReturnActiveGoalForPeriod() {
            when(goalRepository.findActiveGoalsByUserIdAndPeriod(eq(1L), eq(GoalPeriod.WEEKLY), any(LocalDateTime.class)))
                    .thenReturn(List.of(testGoal));

            Optional<Goal> result = goalService.getActiveGoalByPeriod(1L, GoalPeriod.WEEKLY);

            assertThat(result).isPresent();
            assertThat(result.get().getPeriod()).isEqualTo(GoalPeriod.WEEKLY);
        }

        @Test
        @DisplayName("Should return empty when no active goal for period")
        void shouldReturnEmptyWhenNoActiveGoalForPeriod() {
            when(goalRepository.findActiveGoalsByUserIdAndPeriod(eq(1L), eq(GoalPeriod.MONTHLY), any(LocalDateTime.class)))
                    .thenReturn(List.of());

            Optional<Goal> result = goalService.getActiveGoalByPeriod(1L, GoalPeriod.MONTHLY);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createGoal Tests")
    class CreateGoalTests {

        @Test
        @DisplayName("Should create goal successfully")
        void shouldCreateGoalSuccessfully() {
            when(userService.getUserById(1L)).thenReturn(testUser);
            when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> {
                Goal saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(activityRepository.sumDistanceForChallenge(eq(1L), any(), any(), any()))
                    .thenReturn(25.0);

            Goal result = goalService.createGoal(1L, goalRequest);

            assertThat(result).isNotNull();
            assertThat(result.getGoalType()).isEqualTo(GoalType.DISTANCE);
            assertThat(result.getTargetValue()).isEqualTo(100.0);
            assertThat(result.getUser()).isEqualTo(testUser);
            verify(userService).getUserById(1L);
            verify(goalRepository, times(2)).save(any(Goal.class));
        }

        @Test
        @DisplayName("Should create weekly goal with correct date range")
        void shouldCreateWeeklyGoalWithCorrectDateRange() {
            goalRequest.setPeriod(GoalPeriod.WEEKLY);
            when(userService.getUserById(1L)).thenReturn(testUser);
            when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(activityRepository.sumDistanceForChallenge(eq(1L), any(), any(), any()))
                    .thenReturn(0.0);

            Goal result = goalService.createGoal(1L, goalRequest);

            assertThat(result.getPeriod()).isEqualTo(GoalPeriod.WEEKLY);
            assertThat(result.getStartDate()).isNotNull();
            assertThat(result.getEndDate()).isNotNull();
            assertThat(result.getEndDate()).isAfter(result.getStartDate());
        }

        @Test
        @DisplayName("Should create monthly goal with correct date range")
        void shouldCreateMonthlyGoalWithCorrectDateRange() {
            goalRequest.setPeriod(GoalPeriod.MONTHLY);
            when(userService.getUserById(1L)).thenReturn(testUser);
            when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(activityRepository.sumDistanceForChallenge(eq(1L), any(), any(), any()))
                    .thenReturn(0.0);

            Goal result = goalService.createGoal(1L, goalRequest);

            assertThat(result.getPeriod()).isEqualTo(GoalPeriod.MONTHLY);
        }
    }

    @Nested
    @DisplayName("updateGoal Tests")
    class UpdateGoalTests {

        @Test
        @DisplayName("Should update goal successfully when owner")
        void shouldUpdateGoalSuccessfullyWhenOwner() {
            when(goalRepository.findById(1L)).thenReturn(Optional.of(testGoal));
            when(goalRepository.save(any(Goal.class))).thenReturn(testGoal);

            goalRequest.setTargetValue(200.0);
            goalRequest.setGoalType(GoalType.TIME);

            Goal result = goalService.updateGoal(1L, 1L, goalRequest);

            assertThat(result.getTargetValue()).isEqualTo(200.0);
            assertThat(result.getGoalType()).isEqualTo(GoalType.TIME);
            verify(goalRepository).save(testGoal);
        }

        @Test
        @DisplayName("Should throw exception when not owner")
        void shouldThrowExceptionWhenNotOwner() {
            when(goalRepository.findById(1L)).thenReturn(Optional.of(testGoal));

            assertThatThrownBy(() -> goalService.updateGoal(1L, 2L, goalRequest))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(GOAL_ACCESS_DENIED)
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            verify(goalRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when goal not found")
        void shouldThrowExceptionWhenGoalNotFound() {
            when(goalRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> goalService.updateGoal(999L, 1L, goalRequest))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(GOAL_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("deleteGoal Tests")
    class DeleteGoalTests {

        @Test
        @DisplayName("Should delete goal successfully when owner")
        void shouldDeleteGoalSuccessfullyWhenOwner() {
            when(goalRepository.findById(1L)).thenReturn(Optional.of(testGoal));

            goalService.deleteGoal(1L, 1L);

            verify(goalRepository).delete(testGoal);
        }

        @Test
        @DisplayName("Should throw exception when not owner")
        void shouldThrowExceptionWhenNotOwner() {
            when(goalRepository.findById(1L)).thenReturn(Optional.of(testGoal));

            assertThatThrownBy(() -> goalService.deleteGoal(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(GOAL_ACCESS_DENIED);

            verify(goalRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("updateGoalProgress Tests")
    class UpdateGoalProgressTests {

        @Test
        @DisplayName("Should update progress for distance goal")
        void shouldUpdateProgressForDistanceGoal() {
            testGoal.setGoalType(GoalType.DISTANCE);
            when(goalRepository.findActiveGoalsByUserId(1L)).thenReturn(List.of(testGoal));
            when(activityRepository.sumDistanceForChallenge(eq(1L), any(), any(), any()))
                    .thenReturn(75.0);

            goalService.updateGoalProgress(1L);

            assertThat(testGoal.getCurrentValue()).isEqualTo(75.0);
            verify(goalRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should update progress for time goal")
        void shouldUpdateProgressForTimeGoal() {
            testGoal.setGoalType(GoalType.TIME);
            when(goalRepository.findActiveGoalsByUserId(1L)).thenReturn(List.of(testGoal));
            when(activityRepository.sumDurationForChallenge(eq(1L), any(), any(), any()))
                    .thenReturn(7200.0); // 2 hours in seconds

            goalService.updateGoalProgress(1L);

            assertThat(testGoal.getCurrentValue()).isEqualTo(2.0); // 2 hours
            verify(goalRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should update progress for elevation goal")
        void shouldUpdateProgressForElevationGoal() {
            testGoal.setGoalType(GoalType.ELEVATION);
            when(goalRepository.findActiveGoalsByUserId(1L)).thenReturn(List.of(testGoal));
            when(activityRepository.sumElevationForChallenge(eq(1L), any(), any(), any()))
                    .thenReturn(500.0);

            goalService.updateGoalProgress(1L);

            assertThat(testGoal.getCurrentValue()).isEqualTo(500.0);
        }

        @Test
        @DisplayName("Should update progress for activities count goal")
        void shouldUpdateProgressForActivitiesCountGoal() {
            testGoal.setGoalType(GoalType.ACTIVITIES);
            when(goalRepository.findActiveGoalsByUserId(1L)).thenReturn(List.of(testGoal));
            when(activityRepository.countByUserAndActivityTypeAndDateRange(eq(1L), any(), any(), any()))
                    .thenReturn(15);

            goalService.updateGoalProgress(1L);

            assertThat(testGoal.getCurrentValue()).isEqualTo(15.0);
        }

        @Test
        @DisplayName("Should handle empty activity data")
        void shouldHandleEmptyActivityData() {
            testGoal.setGoalType(GoalType.DISTANCE);
            when(goalRepository.findActiveGoalsByUserId(1L)).thenReturn(List.of(testGoal));
            when(activityRepository.sumDistanceForChallenge(eq(1L), any(), any(), any()))
                    .thenReturn(0.0);

            goalService.updateGoalProgress(1L);

            assertThat(testGoal.getCurrentValue()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should update multiple goals")
        void shouldUpdateMultipleGoals() {
            Goal goal2 = TestDataFactory.createGoal(2L, testUser);
            goal2.setGoalType(GoalType.TIME);

            when(goalRepository.findActiveGoalsByUserId(1L)).thenReturn(List.of(testGoal, goal2));
            when(activityRepository.sumDistanceForChallenge(eq(1L), any(), any(), any()))
                    .thenReturn(50.0);
            when(activityRepository.sumDurationForChallenge(eq(1L), any(), any(), any()))
                    .thenReturn(3600.0);

            goalService.updateGoalProgress(1L);

            assertThat(testGoal.getCurrentValue()).isEqualTo(50.0);
            assertThat(goal2.getCurrentValue()).isEqualTo(1.0); // 1 hour
            verify(goalRepository).saveAll(anyList());
        }
    }
}
