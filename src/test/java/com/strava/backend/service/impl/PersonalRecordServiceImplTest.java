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
        @DisplayName("Should return empty list when duration is zero")
        void shouldReturnEmptyWhenDurationZero() {
            Activity activity = runActivity(1L, 10.0, 0);

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            assertThat(result).isEmpty();
            verifyNoInteractions(personalRecordRepository);
        }

        @Test
        @DisplayName("Should create new PRs when none exist (first 10km activity)")
        void shouldCreateNewPRsWhenNoneExist() {
            Activity activity = runActivity(1L, 10.0, 3600); // 10km in 60 min

            when(personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    eq(1L), eq(ActivityType.RUN), any(DistanceType.class)))
                    .thenReturn(Optional.empty());
            when(personalRecordRepository.save(any(PersonalRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            // 10km activity should create PRs for 1K, 5K, and 10K
            assertThat(result).hasSize(3);
            assertThat(result).extracting(PersonalRecord::getDistanceType)
                    .containsExactlyInAnyOrder(DistanceType.ONE_KM, DistanceType.FIVE_KM, DistanceType.TEN_KM);
            assertThat(result).allSatisfy(pr -> {
                assertThat(pr.getUser()).isEqualTo(testUser);
                assertThat(pr.getActivity()).isEqualTo(activity);
                assertThat(pr.getActivityType()).isEqualTo(ActivityType.RUN);
                assertThat(pr.getTimeInSeconds()).isPositive();
            });
            verify(personalRecordRepository, times(3)).save(any(PersonalRecord.class));
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
            when(personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    eq(1L), eq(ActivityType.RUN), eq(DistanceType.ONE_KM)))
                    .thenReturn(Optional.empty());
            when(personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    eq(1L), eq(ActivityType.RUN), eq(DistanceType.FIVE_KM)))
                    .thenReturn(Optional.empty());
            when(personalRecordRepository.save(any(PersonalRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            // All three PRs should be saved (two new, one updated)
            assertThat(result).hasSize(3);
            // The 10km PR should now be 1800 (faster), with previousTime = 3000
            PersonalRecord updated10K = result.stream()
                    .filter(pr -> pr.getDistanceType() == DistanceType.TEN_KM)
                    .findFirst()
                    .orElseThrow();
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
            PersonalRecord existing5K = PersonalRecord.builder()
                    .user(testUser)
                    .activity(activity)
                    .activityType(ActivityType.RUN)
                    .distanceType(DistanceType.FIVE_KM)
                    .timeInSeconds(1400)
                    .distance(5.0)
                    .achievedAt(testUser.getCreatedAt())
                    .build();
            PersonalRecord existing1K = PersonalRecord.builder()
                    .user(testUser)
                    .activity(activity)
                    .activityType(ActivityType.RUN)
                    .distanceType(DistanceType.ONE_KM)
                    .timeInSeconds(250)
                    .distance(1.0)
                    .achievedAt(testUser.getCreatedAt())
                    .build();

            when(personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    1L, ActivityType.RUN, DistanceType.TEN_KM))
                    .thenReturn(Optional.of(existing10K));
            when(personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    1L, ActivityType.RUN, DistanceType.FIVE_KM))
                    .thenReturn(Optional.of(existing5K));
            when(personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    1L, ActivityType.RUN, DistanceType.ONE_KM))
                    .thenReturn(Optional.of(existing1K));

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            assertThat(result).isEmpty();
            verify(personalRecordRepository, never()).save(any(PersonalRecord.class));
        }

        @Test
        @DisplayName("Should create PRs for ONE_KM, FIVE_KM and TEN_KM for a 10km activity")
        void shouldCreateMultiplePRsForLongerRun() {
            Activity activity = runActivity(1L, 10.0, 3000); // 10km in 50 min

            when(personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    eq(1L), eq(ActivityType.RUN), any(DistanceType.class)))
                    .thenReturn(Optional.empty());
            when(personalRecordRepository.save(any(PersonalRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            assertThat(result).hasSize(3);
            assertThat(result).extracting(PersonalRecord::getDistanceType)
                    .containsExactlyInAnyOrder(DistanceType.ONE_KM, DistanceType.FIVE_KM, DistanceType.TEN_KM);
            // NO half marathon or marathon PRs since distance is only 10km
            assertThat(result).extracting(PersonalRecord::getDistanceType)
                    .doesNotContain(DistanceType.HALF_MARATHON, DistanceType.MARATHON);
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
