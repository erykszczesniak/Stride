package com.strava.challengegoal.goal;

import com.strava.challengegoal.client.ActivityClient;
import com.strava.challengegoal.client.ActivitySummary;
import com.strava.challengegoal.common.ActivityType;
import com.strava.challengegoal.exception.ForbiddenException;
import com.strava.challengegoal.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoalService Unit Tests")
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private ActivityClient activityClient;

    @InjectMocks
    private GoalService goalService;

    private Goal testGoal;
    private GoalRequest goalRequest;

    @BeforeEach
    void setUp() {
        testGoal = Goal.builder()
                .id(1L)
                .goalType(GoalType.DISTANCE)
                .period(GoalPeriod.WEEKLY)
                .activityType(ActivityType.RUN)
                .targetValue(50.0)
                .currentValue(25.0)
                .startDate(LocalDateTime.now().minusDays(3))
                .endDate(LocalDateTime.now().plusDays(4))
                .isActive(true)
                .userId(1L)
                .build();

        goalRequest = new GoalRequest();
        goalRequest.setGoalType(GoalType.DISTANCE);
        goalRequest.setPeriod(GoalPeriod.WEEKLY);
        goalRequest.setActivityType(ActivityType.RUN);
        goalRequest.setTargetValue(100.0);
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
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Goal not found");
        }
    }

    @Nested
    @DisplayName("getActiveGoalsByUserId Tests")
    class GetActiveGoalsByUserIdTests {

        @Test
        @DisplayName("Should return active goals with refreshed progress")
        void shouldReturnActiveGoals() {
            when(goalRepository.findActiveGoalsByUserId(1L)).thenReturn(List.of(testGoal));
            when(activityClient.summaryFor(eq(1L), eq("RUN"), any(), any()))
                    .thenReturn(new ActivitySummary(42.0, 0, 0, 0));

            List<Goal> result = goalService.getActiveGoalsByUserId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getCurrentValue()).isEqualTo(42.0);
            verify(goalRepository).saveAll(anyList());
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
            Goal goal2 = Goal.builder().id(2L).goalType(GoalType.TIME).period(GoalPeriod.MONTHLY)
                    .targetValue(10.0).startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(30))
                    .userId(1L).build();
            when(goalRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(testGoal, goal2));

            List<Goal> result = goalService.getAllGoalsByUserId(1L);

            assertThat(result).hasSize(2);
            verify(goalRepository).findByUserIdOrderByCreatedAtDesc(1L);
        }
    }

    @Nested
    @DisplayName("createGoal Tests")
    class CreateGoalTests {

        @Test
        @DisplayName("Should create goal successfully")
        void shouldCreateGoalSuccessfully() {
            when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> {
                Goal saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(activityClient.summaryFor(eq(1L), eq("RUN"), any(), any()))
                    .thenReturn(new ActivitySummary(25.0, 0, 0, 0));

            Goal result = goalService.createGoal(1L, goalRequest);

            assertThat(result).isNotNull();
            assertThat(result.getGoalType()).isEqualTo(GoalType.DISTANCE);
            assertThat(result.getTargetValue()).isEqualTo(100.0);
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getCurrentValue()).isEqualTo(25.0);
            verify(goalRepository, times(2)).save(any(Goal.class));
        }

        @Test
        @DisplayName("Should create weekly goal with correct date range")
        void shouldCreateWeeklyGoalWithCorrectDateRange() {
            goalRequest.setPeriod(GoalPeriod.WEEKLY);
            when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(activityClient.summaryFor(eq(1L), eq("RUN"), any(), any()))
                    .thenReturn(ActivitySummary.EMPTY);

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
            when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(activityClient.summaryFor(eq(1L), eq("RUN"), any(), any()))
                    .thenReturn(ActivitySummary.EMPTY);

            Goal result = goalService.createGoal(1L, goalRequest);

            assertThat(result.getPeriod()).isEqualTo(GoalPeriod.MONTHLY);
            assertThat(result.getStartDate().getDayOfMonth()).isEqualTo(1);
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
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("You don't have permission to access this goal");

            verify(goalRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when goal not found")
        void shouldThrowExceptionWhenGoalNotFound() {
            when(goalRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> goalService.updateGoal(999L, 1L, goalRequest))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Goal not found");
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
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("You don't have permission to access this goal");

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
            when(activityClient.summaryFor(eq(1L), eq("RUN"), any(), any()))
                    .thenReturn(new ActivitySummary(75.0, 0, 0, 0));

            goalService.updateGoalProgress(1L);

            assertThat(testGoal.getCurrentValue()).isEqualTo(75.0);
            verify(goalRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should update progress for time goal")
        void shouldUpdateProgressForTimeGoal() {
            testGoal.setGoalType(GoalType.TIME);
            when(goalRepository.findActiveGoalsByUserId(1L)).thenReturn(List.of(testGoal));
            when(activityClient.summaryFor(eq(1L), eq("RUN"), any(), any()))
                    .thenReturn(new ActivitySummary(0, 7200, 0, 0)); // 2 hours in seconds

            goalService.updateGoalProgress(1L);

            assertThat(testGoal.getCurrentValue()).isEqualTo(2.0); // 2 hours
        }

        @Test
        @DisplayName("Should update progress for elevation goal")
        void shouldUpdateProgressForElevationGoal() {
            testGoal.setGoalType(GoalType.ELEVATION);
            when(goalRepository.findActiveGoalsByUserId(1L)).thenReturn(List.of(testGoal));
            when(activityClient.summaryFor(eq(1L), eq("RUN"), any(), any()))
                    .thenReturn(new ActivitySummary(0, 0, 500.0, 0));

            goalService.updateGoalProgress(1L);

            assertThat(testGoal.getCurrentValue()).isEqualTo(500.0);
        }

        @Test
        @DisplayName("Should update progress for activities count goal")
        void shouldUpdateProgressForActivitiesCountGoal() {
            testGoal.setGoalType(GoalType.ACTIVITIES);
            when(goalRepository.findActiveGoalsByUserId(1L)).thenReturn(List.of(testGoal));
            when(activityClient.summaryFor(eq(1L), eq("RUN"), any(), any()))
                    .thenReturn(new ActivitySummary(0, 0, 0, 15));

            goalService.updateGoalProgress(1L);

            assertThat(testGoal.getCurrentValue()).isEqualTo(15.0);
        }

        @Test
        @DisplayName("Should handle empty activity data")
        void shouldHandleEmptyActivityData() {
            testGoal.setGoalType(GoalType.DISTANCE);
            when(goalRepository.findActiveGoalsByUserId(1L)).thenReturn(List.of(testGoal));
            when(activityClient.summaryFor(eq(1L), eq("RUN"), any(), any()))
                    .thenReturn(ActivitySummary.EMPTY);

            goalService.updateGoalProgress(1L);

            assertThat(testGoal.getCurrentValue()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should update multiple goals")
        void shouldUpdateMultipleGoals() {
            Goal goal2 = Goal.builder()
                    .id(2L)
                    .goalType(GoalType.TIME)
                    .period(GoalPeriod.WEEKLY)
                    .activityType(ActivityType.RIDE)
                    .targetValue(10.0)
                    .startDate(LocalDateTime.now().minusDays(3))
                    .endDate(LocalDateTime.now().plusDays(4))
                    .userId(1L)
                    .build();

            when(goalRepository.findActiveGoalsByUserId(1L)).thenReturn(List.of(testGoal, goal2));
            when(activityClient.summaryFor(eq(1L), eq("RUN"), any(), any()))
                    .thenReturn(new ActivitySummary(50.0, 0, 0, 0));
            when(activityClient.summaryFor(eq(1L), eq("RIDE"), any(), any()))
                    .thenReturn(new ActivitySummary(0, 3600, 0, 0));

            goalService.updateGoalProgress(1L);

            assertThat(testGoal.getCurrentValue()).isEqualTo(50.0);
            assertThat(goal2.getCurrentValue()).isEqualTo(1.0); // 1 hour
            verify(goalRepository).saveAll(anyList());
        }
    }
}
