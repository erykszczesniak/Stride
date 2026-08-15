package com.strava.activityservice.pr;

import com.strava.activityservice.client.UserVisibilityClient;
import com.strava.activityservice.client.UserVisibilityClient.VisibilityInfo;
import com.strava.activityservice.domain.*;
import com.strava.activityservice.event.ActivityEventPublisher;
import com.strava.activityservice.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersonalRecordService Unit Tests")
class PersonalRecordServiceTest {

    @Mock
    private PersonalRecordRepository personalRecordRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserVisibilityClient userVisibilityClient;

    @Mock
    private ActivityEventPublisher eventPublisher;

    @InjectMocks
    private PersonalRecordService personalRecordService;

    private Activity runActivity(Long id, double distanceKm, int durationSeconds) {
        Activity activity = TestDataFactory.createActivity(id, 1L);
        activity.setActivityType(ActivityType.RUN);
        activity.setDistance(distanceKm);
        activity.setDuration(durationSeconds);
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
            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(runActivity(1L, 0.0, 3600));

            assertThat(result).isEmpty();
            verifyNoInteractions(personalRecordRepository);
        }

        @Test
        @DisplayName("Should return empty list when duration is null or zero")
        void shouldReturnEmptyWhenDurationMissing() {
            Activity activity = runActivity(1L, 10.0, 3600);
            activity.setDuration(null);
            assertThat(personalRecordService.checkAndUpdatePRs(activity)).isEmpty();

            assertThat(personalRecordService.checkAndUpdatePRs(runActivity(1L, 10.0, 0))).isEmpty();
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
        @DisplayName("Should create one PR for the largest distance fit (first 10km activity -> 10K)")
        void shouldCreateNewPRWhenNoneExists() {
            Activity activity = runActivity(1L, 10.0, 3600); // 10km in 60 min

            when(personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    eq(1L), eq(ActivityType.RUN), any(DistanceType.class)))
                    .thenReturn(Optional.empty());
            when(personalRecordRepository.save(any(PersonalRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            // 10km activity -> only TEN_KM PR (not 1K/5K — no per-segment timing to prove those)
            assertThat(result).hasSize(1);
            PersonalRecord pr = result.get(0);
            assertThat(pr.getDistanceType()).isEqualTo(DistanceType.TEN_KM);
            assertThat(pr.getUserId()).isEqualTo(1L);
            assertThat(pr.getActivityId()).isEqualTo(1L);
            assertThat(pr.getActivityType()).isEqualTo(ActivityType.RUN);
            assertThat(pr.getTimeInSeconds()).isEqualTo(3600);
            assertThat(pr.getAchievedAt()).isEqualTo(activity.getStartTime());
            verify(personalRecordRepository, times(1)).save(any(PersonalRecord.class));
            verify(eventPublisher).publishPersonalRecordAchieved(pr);
        }

        @Test
        @DisplayName("Should update existing PR only when new time is faster")
        void shouldUpdateExistingPRWhenFaster() {
            Activity activity = runActivity(1L, 10.0, 1800); // 10km in 30 min (fast!)

            PersonalRecord existing10K = PersonalRecord.builder()
                    .userId(1L)
                    .activityId(99L)
                    .activityType(ActivityType.RUN)
                    .distanceType(DistanceType.TEN_KM)
                    .timeInSeconds(3000) // previous PR: 50 min
                    .distance(10.0)
                    .achievedAt(activity.getStartTime().minusDays(30))
                    .build();

            when(personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    1L, ActivityType.RUN, DistanceType.TEN_KM))
                    .thenReturn(Optional.of(existing10K));
            when(personalRecordRepository.save(any(PersonalRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            assertThat(result).hasSize(1);
            PersonalRecord updated10K = result.get(0);
            assertThat(updated10K.getDistanceType()).isEqualTo(DistanceType.TEN_KM);
            assertThat(updated10K.getTimeInSeconds()).isEqualTo(1800);
            assertThat(updated10K.getPreviousTimeInSeconds()).isEqualTo(3000);
            assertThat(updated10K.getActivityId()).isEqualTo(1L);
            verify(eventPublisher).publishPersonalRecordAchieved(updated10K);
        }

        @Test
        @DisplayName("Should NOT update existing PR when new time is slower")
        void shouldNotUpdatePRWhenSlower() {
            Activity activity = runActivity(1L, 10.0, 5400); // 10km in 90 min (slow)

            PersonalRecord existing10K = PersonalRecord.builder()
                    .userId(1L)
                    .activityId(99L)
                    .activityType(ActivityType.RUN)
                    .distanceType(DistanceType.TEN_KM)
                    .timeInSeconds(3000)
                    .distance(10.0)
                    .achievedAt(activity.getStartTime().minusDays(30))
                    .build();
            when(personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    1L, ActivityType.RUN, DistanceType.TEN_KM))
                    .thenReturn(Optional.of(existing10K));

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            assertThat(result).isEmpty();
            verify(personalRecordRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("A half-marathon-plus run registers HALF_MARATHON, not MARATHON")
        void halfMarathonFit() {
            Activity activity = runActivity(1L, 32.2, 10800);

            when(personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    eq(1L), eq(ActivityType.RUN), any(DistanceType.class)))
                    .thenReturn(Optional.empty());
            when(personalRecordRepository.save(any(PersonalRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDistanceType()).isEqualTo(DistanceType.HALF_MARATHON);
            // 21.0975/32.2 * 10800 = 7076s at average pace
            assertThat(result.get(0).getTimeInSeconds()).isEqualTo(7076);
        }

        @Test
        @DisplayName("A 1.5km swim registers the largest fitting pool/open-water distance (1K)")
        void swimLargestFit() {
            Activity activity = runActivity(1L, 1.5, 2100);
            activity.setActivityType(ActivityType.SWIM);

            when(personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    eq(1L), eq(ActivityType.SWIM), any(DistanceType.class)))
                    .thenReturn(Optional.empty());
            when(personalRecordRepository.save(any(PersonalRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDistanceType()).isEqualTo(DistanceType.ONE_KM);
        }

        @Test
        @DisplayName("Walks use the running distance set (a 5km walk sets a FIVE_KM PR)")
        void walkUsesRunningSet() {
            Activity activity = runActivity(1L, 5.0, 3600);
            activity.setActivityType(ActivityType.WALK);

            when(personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    eq(1L), eq(ActivityType.WALK), any(DistanceType.class)))
                    .thenReturn(Optional.empty());
            when(personalRecordRepository.save(any(PersonalRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDistanceType()).isEqualTo(DistanceType.FIVE_KM);
        }

        @Test
        @DisplayName("Rides cap at TEN_KM (no half marathon for cycling)")
        void rideCapsAtTenKm() {
            Activity activity = runActivity(1L, 45.2, 7200);
            activity.setActivityType(ActivityType.RIDE);

            when(personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    eq(1L), eq(ActivityType.RIDE), any(DistanceType.class)))
                    .thenReturn(Optional.empty());
            when(personalRecordRepository.save(any(PersonalRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<PersonalRecord> result = personalRecordService.checkAndUpdatePRs(activity);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDistanceType()).isEqualTo(DistanceType.TEN_KM);
        }
    }

    @Nested
    @DisplayName("getUserPRsVisibleTo Tests")
    class GetUserPRsVisibleToTests {

        @Test
        @DisplayName("Users always see their own PRs without a remote privacy check")
        void ownSeesOwn() {
            when(personalRecordRepository.findByUserId(1L)).thenReturn(List.of());

            personalRecordService.getUserPRsVisibleTo(1L, 1L);

            verify(personalRecordRepository).findByUserId(1L);
            verifyNoInteractions(userVisibilityClient);
        }

        @Test
        @DisplayName("Private target hides PRs from strangers but not followers")
        void privateTargetNeedsFollow() {
            when(userVisibilityClient.visibilityFor(2L, 1L))
                    .thenReturn(new VisibilityInfo(true, false));
            assertThat(personalRecordService.getUserPRsVisibleTo(2L, 1L)).isEmpty();

            when(userVisibilityClient.visibilityFor(2L, 1L))
                    .thenReturn(new VisibilityInfo(true, true));
            when(personalRecordRepository.findByUserId(2L)).thenReturn(List.of());
            personalRecordService.getUserPRsVisibleTo(2L, 1L);
            verify(personalRecordRepository).findByUserId(2L);
        }

        @Test
        @DisplayName("Public target is visible to anyone")
        void publicTargetVisible() {
            when(userVisibilityClient.visibilityFor(2L, null))
                    .thenReturn(new VisibilityInfo(false, false));
            when(personalRecordRepository.findByUserId(2L)).thenReturn(List.of());

            personalRecordService.getUserPRsVisibleTo(2L, null);

            verify(personalRecordRepository).findByUserId(2L);
        }
    }

    @Nested
    @DisplayName("recalculateAllPRs Tests")
    class RecalculateAllPRsTests {

        @Test
        @DisplayName("Wipes existing PRs first, then replays activities chronologically")
        void wipesThenReplays() {
            Activity first = runActivity(1L, 5.0, 1500);
            Activity second = runActivity(2L, 10.0, 3600);
            when(activityRepository.findAllByUserIdOrderByStartTimeAsc(1L))
                    .thenReturn(List.of(first, second));
            when(personalRecordRepository.findByUserIdAndActivityTypeAndDistanceType(
                    eq(1L), eq(ActivityType.RUN), any(DistanceType.class)))
                    .thenReturn(Optional.empty());
            when(personalRecordRepository.save(any(PersonalRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            personalRecordService.recalculateAllPRs(1L);

            InOrder inOrder = inOrder(personalRecordRepository);
            inOrder.verify(personalRecordRepository).deleteByUserId(1L);
            inOrder.verify(personalRecordRepository, times(2)).save(any(PersonalRecord.class));
        }
    }
}
