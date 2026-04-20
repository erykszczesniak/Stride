package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.PersonalRecord;
import com.strava.backend.domain.User;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.DistanceType;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.PersonalRecordRepository;
import com.strava.backend.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersonalRecordServiceImpl Unit Tests")
class PersonalRecordServiceImplTest {

    @Mock
    private PersonalRecordRepository personalRecordRepository;

    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private PersonalRecordServiceImpl personalRecordService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createUser(1L, "john@example.com", "John", "Doe");
    }

    private Activity runActivity(Long id, double distanceKm, int durationSeconds) {
        Activity activity = TestDataFactory.createActivity(id, testUser);
        activity.setActivityType(ActivityType.RUN);
        activity.setDistance(distanceKm);
        activity.setDuration(durationSeconds);
        activity.setRoutePolyline(null); // force pace-estimation path
        return activity;
    }

    @Nested
    @DisplayName("checkAndUpdatePRs Tests")
    class CheckAndUpdatePRsTests {

        @Test
        @DisplayName("Should return empty list when distance is null")
        void shouldReturnEmptyWhenDistanceNull() {
            Activity activity = runActivity(1L, 10.0, 3600);
            activity.setDistance(null);

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            assertThat(result).isEmpty();
            verifyNoInteractions(personalRecordRepository);
        }

        @Test
        @DisplayName("Should return empty list when distance is zero")
        void shouldReturnEmptyWhenDistanceZero() {
            Activity activity = runActivity(1L, 0.0, 3600);

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            assertThat(result).isEmpty();
            verifyNoInteractions(personalRecordRepository);
        }

        @Test
        @DisplayName("Should return empty list when duration is null")
        void shouldReturnEmptyWhenDurationNull() {
            Activity activity = runActivity(1L, 10.0, 3600);
            activity.setDuration(null);

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            assertThat(result).isEmpty();
            verifyNoInteractions(personalRecordRepository);
        }

        @Test
        @DisplayName("Should return empty list for non-distance activity types (e.g. WORKOUT)")
        void shouldReturnEmptyForWorkout() {
            Activity activity = runActivity(1L, 10.0, 3600);
            activity.setActivityType(ActivityType.WORKOUT);

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            assertThat(result).isEmpty();
            verifyNoInteractions(personalRecordRepository);
        }


        @Test
        @DisplayName("Should return empty list when duration is zero")
        void shouldReturnEmptyWhenDurationZero() {
            Activity activity = runActivity(1L, 10.0, 0);

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            assertThat(result).isEmpty();
            verifyNoInteractions(personalRecordRepository);
        }

        @Test
        @DisplayName("Should create one PR for the largest distance fit (first 10km activity → 10K)")
        void shouldCreateNewPRsWhenNoneExist() {
            Activity activity = runActivity(1L, 10.0, 3600); // 10km in 60 min

            when(personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    eq(1L), eq(ActivityType.RUN), any(DistanceType.class)))
                    .thenReturn(Optional.empty());
            when(personalRecordRepository.save(any(PersonalRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            // 10km activity → only TEN_KM PR (not 1K/5K — no per-segment timing to prove those)
            assertThat(result).hasSize(1);
            PersonalRecord pr = result.get(0);
            assertThat(pr.getDistanceType()).isEqualTo(DistanceType.TEN_KM);
            assertThat(pr.getUser()).isEqualTo(testUser);
            assertThat(pr.getActivity()).isEqualTo(activity);
            assertThat(pr.getActivityType()).isEqualTo(ActivityType.RUN);
            assertThat(pr.getTimeInSeconds()).isEqualTo(3600);
            verify(personalRecordRepository, times(1)).save(any(PersonalRecord.class));
        }

        @Test
        @DisplayName("Should update existing PR only when new time is faster")
        void shouldUpdateExistingPRWhenFaster() {
            Activity activity = runActivity(1L, 10.0, 1800); // 10km in 30 min (fast!)

            PersonalRecord existing10K = PersonalRecord.builder()
                    .user(testUser)
                    .activity(activity)
                    .activityType(ActivityType.RUN)
                    .distanceType(DistanceType.TEN_KM)
                    .timeInSeconds(3000) // previous PR: 50 min
                    .distance(10.0)
                    .achievedAt(testUser.getCreatedAt())
                    .build();

            when(personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    1L, ActivityType.RUN, DistanceType.TEN_KM))
                    .thenReturn(Optional.of(existing10K));
            when(personalRecordRepository.save(any(PersonalRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            // Only the TEN_KM PR — no 1K/5K PRs any more (no per-segment timing
            // means we can't honestly claim a sub-distance PR from this activity).
            assertThat(result).hasSize(1);
            PersonalRecord updated10K = result.get(0);
            assertThat(updated10K.getDistanceType()).isEqualTo(DistanceType.TEN_KM);
            assertThat(updated10K.getTimeInSeconds()).isEqualTo(1800);
            assertThat(updated10K.getPreviousTimeInSeconds()).isEqualTo(3000);
        }

        @Test
        @DisplayName("Should NOT update existing PR when new time is slower")
        void shouldNotUpdatePRWhenSlower() {
            Activity activity = runActivity(1L, 10.0, 5400); // 10km in 90 min (slow)

            PersonalRecord existing10K = PersonalRecord.builder()
                    .user(testUser)
                    .activity(activity)
                    .activityType(ActivityType.RUN)
                    .distanceType(DistanceType.TEN_KM)
                    .timeInSeconds(3000) // previous PR: 50 min
                    .distance(10.0)
                    .achievedAt(testUser.getCreatedAt())
                    .build();
            when(personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    1L, ActivityType.RUN, DistanceType.TEN_KM))
                    .thenReturn(Optional.of(existing10K));

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            assertThat(result).isEmpty();
            verify(personalRecordRepository, never()).save(any(PersonalRecord.class));
        }

        @Test
        @DisplayName("Should create a TEN_KM PR for a 10km activity (no shorter duplicates)")
        void shouldCreateMultiplePRsForLongerRun() {
            Activity activity = runActivity(1L, 10.0, 3000); // 10km in 50 min

            when(personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    eq(1L), eq(ActivityType.RUN), any(DistanceType.class)))
                    .thenReturn(Optional.empty());
            when(personalRecordRepository.save(any(PersonalRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            // Only TEN_KM — largest fit within the 10 km activity. No 1K/5K
            // from the same activity (can't prove a sub-distance PR without
            // per-segment timing) and no HM/Marathon (distance too short).
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDistanceType()).isEqualTo(DistanceType.TEN_KM);
        }

        @Test
        @DisplayName("Should create no PRs for a 500m run (below all thresholds)")
        void shouldCreateNoPRsForShortRun() {
            Activity activity = runActivity(1L, 0.5, 180); // 500m in 3 min

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            assertThat(result).isEmpty();
            verify(personalRecordRepository, never()).save(any(PersonalRecord.class));
            verify(personalRecordRepository, never())
                    .findByUserIdAndActivityTypeAndDistanceType(anyLong(), any(), any());
        }
    }

    @Nested
    @DisplayName("recalculateAllPRs Tests")
    class RecalculateAllPRsTests {

        @Test
        @DisplayName("Should call deleteByUserId first then iterate activities")
        void shouldDeleteThenIterate() {
            Activity a1 = runActivity(1L, 10.0, 3600);
            Activity a2 = runActivity(2L, 21.5, 7200);
            when(activityRepository.findAllByUserIdOrderByStartTimeAsc(1L))
                    .thenReturn(List.of(a1, a2));
            when(personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    eq(1L), any(), any()))
                    .thenReturn(Optional.empty());
            when(personalRecordRepository.save(any(PersonalRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            personalRecordService.recalculateAllPRs(1L);

            // Verify ordering: delete must happen before activities are fetched
            var inOrder = inOrder(personalRecordRepository, activityRepository);
            inOrder.verify(personalRecordRepository).deleteByUserId(1L);
            inOrder.verify(activityRepository).findAllByUserIdOrderByStartTimeAsc(1L);
        }

        @Test
        @DisplayName("Should delete and do nothing when user has no activities")
        void shouldHandleEmptyActivityList() {
            when(activityRepository.findAllByUserIdOrderByStartTimeAsc(1L))
                    .thenReturn(Collections.emptyList());

            personalRecordService.recalculateAllPRs(1L);

            verify(personalRecordRepository).deleteByUserId(1L);
            verify(activityRepository).findAllByUserIdOrderByStartTimeAsc(1L);
            verify(personalRecordRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getUserPRs Tests")
    class GetUserPRsTests {

        @Test
        @DisplayName("Should delegate to repository")
        void shouldDelegateToRepository() {
            PersonalRecord pr = PersonalRecord.builder()
                    .user(testUser)
                    .activityType(ActivityType.RUN)
                    .distanceType(DistanceType.FIVE_KM)
                    .timeInSeconds(1400)
                    .distance(5.0)
                    .build();
            when(personalRecordRepository.findByUserId(1L)).thenReturn(List.of(pr));

            List<PersonalRecord> result = personalRecordService.getUserPRs(1L);

            assertThat(result).containsExactly(pr);
            verify(personalRecordRepository).findByUserId(1L);
        }
    }

    @Nested
    @DisplayName("getUserPRsByActivityType Tests")
    class GetUserPRsByActivityTypeTests {

        @Test
        @DisplayName("Should delegate to repository")
        void shouldDelegateToRepository() {
            PersonalRecord pr = PersonalRecord.builder()
                    .user(testUser)
                    .activityType(ActivityType.RUN)
                    .distanceType(DistanceType.TEN_KM)
                    .timeInSeconds(3000)
                    .distance(10.0)
                    .build();
            when(personalRecordRepository.findByUserIdAndActivityType(1L, ActivityType.RUN))
                    .thenReturn(List.of(pr));

            List<PersonalRecord> result = personalRecordService.getUserPRsByActivityType(1L, ActivityType.RUN);

            assertThat(result).containsExactly(pr);
            verify(personalRecordRepository).findByUserIdAndActivityType(1L, ActivityType.RUN);
        }
    }
}
