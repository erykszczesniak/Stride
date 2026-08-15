package com.strava.challengegoal.challenge;

import com.strava.challengegoal.client.ActivityClient;
import com.strava.challengegoal.client.ActivitySummary;
import com.strava.challengegoal.client.UserBatchClient;
import com.strava.challengegoal.client.UserSummary;
import com.strava.challengegoal.common.ActivityType;
import com.strava.challengegoal.exception.BadRequestException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengeService Unit Tests")
class ChallengeServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ActivityClient activityClient;

    @Mock
    private UserBatchClient userBatchClient;

    @InjectMocks
    private ChallengeService challengeService;

    private Challenge testChallenge;

    @BeforeEach
    void setUp() {
        testChallenge = Challenge.builder()
                .id(1L)
                .name("Monthly Running Challenge")
                .description("Run 100km this month")
                .challengeType(ChallengeType.DISTANCE)
                .activityType(ActivityType.RUN)
                .targetValue(100.0)
                .startDate(LocalDateTime.now().minusDays(5))
                .endDate(LocalDateTime.now().plusDays(25))
                .isPublic(true)
                .creatorId(1L)
                .build();
    }

    @Nested
    @DisplayName("getChallengeById Tests")
    class GetChallengeByIdTests {

        @Test
        @DisplayName("Should return challenge when found")
        void shouldReturnChallengeWhenFound() {
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));

            Challenge result = challengeService.getChallengeById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Monthly Running Challenge");
        }

        @Test
        @DisplayName("Should throw exception when challenge not found")
        void shouldThrowExceptionWhenChallengeNotFound() {
            given(challengeRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> challengeService.getChallengeById(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Challenge not found");
        }
    }

    @Nested
    @DisplayName("getActiveChallenges / getUpcomingChallenges Tests")
    class ListingTests {

        @Test
        @DisplayName("Should return paginated active challenges")
        void shouldReturnPaginatedActiveChallenges() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Challenge> challengePage = new PageImpl<>(List.of(testChallenge));
            given(challengeRepository.findActivePublicChallenges(any(LocalDateTime.class), eq(pageable)))
                    .willReturn(challengePage);

            Page<Challenge> result = challengeService.getActiveChallenges(pageable);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should return upcoming challenges")
        void shouldReturnUpcomingChallenges() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Challenge> challengePage = new PageImpl<>(List.of(testChallenge));
            given(challengeRepository.findUpcomingPublicChallenges(any(LocalDateTime.class), eq(pageable)))
                    .willReturn(challengePage);

            Page<Challenge> result = challengeService.getUpcomingChallenges(pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("createChallenge Tests")
    class CreateChallengeTests {

        @Test
        @DisplayName("Should create challenge with creator as participant")
        void shouldCreateChallengeSuccessfully() {
            Challenge newChallenge = Challenge.builder()
                    .name("New Challenge")
                    .challengeType(ChallengeType.DISTANCE)
                    .activityType(ActivityType.RUN)
                    .targetValue(50.0)
                    .startDate(LocalDateTime.now().plusDays(1))
                    .endDate(LocalDateTime.now().plusDays(30))
                    .isPublic(true)
                    .build();

            given(challengeRepository.save(any(Challenge.class))).willAnswer(invocation -> {
                Challenge saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            Challenge result = challengeService.createChallenge(newChallenge, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getCreatorId()).isEqualTo(1L);
            assertThat(result.getParticipantIds()).contains(1L);
            then(challengeRepository).should().save(any(Challenge.class));
        }
    }

    @Nested
    @DisplayName("updateChallenge Tests")
    class UpdateChallengeTests {

        @Test
        @DisplayName("Should update challenge when user is creator and challenge not started")
        void shouldUpdateChallengeWhenUserIsCreator() {
            testChallenge.setStartDate(LocalDateTime.now().plusDays(5)); // Not started yet
            Challenge updatedData = Challenge.builder()
                    .name("Updated Challenge")
                    .description("Updated description")
                    .challengeType(ChallengeType.TIME)
                    .activityType(ActivityType.RIDE)
                    .targetValue(100.0)
                    .startDate(LocalDateTime.now().plusDays(10))
                    .endDate(LocalDateTime.now().plusDays(40))
                    .isPublic(false)
                    .build();

            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(challengeRepository.save(any(Challenge.class))).willReturn(testChallenge);

            Challenge result = challengeService.updateChallenge(1L, updatedData, 1L);

            assertThat(result.getName()).isEqualTo("Updated Challenge");
            then(challengeRepository).should().save(testChallenge);
        }

        @Test
        @DisplayName("Should throw exception when user is not creator")
        void shouldThrowExceptionWhenUserIsNotCreator() {
            Challenge updatedData = Challenge.builder().name("Updated").build();
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));

            assertThatThrownBy(() -> challengeService.updateChallenge(1L, updatedData, 2L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Only the creator can update this challenge");
        }

        @Test
        @DisplayName("Should throw exception when challenge has already started")
        void shouldThrowExceptionWhenChallengeStarted() {
            testChallenge.setStartDate(LocalDateTime.now().minusDays(1)); // Already started
            Challenge updatedData = Challenge.builder().name("Updated").build();
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));

            assertThatThrownBy(() -> challengeService.updateChallenge(1L, updatedData, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Cannot update a challenge that has already started");
        }
    }

    @Nested
    @DisplayName("deleteChallenge Tests")
    class DeleteChallengeTests {

        @Test
        @DisplayName("Should delete challenge when user is creator")
        void shouldDeleteChallengeWhenUserIsCreator() {
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));

            challengeService.deleteChallenge(1L, 1L);

            then(challengeRepository).should().delete(testChallenge);
        }

        @Test
        @DisplayName("Should throw exception when user is not creator")
        void shouldThrowExceptionWhenUserIsNotCreator() {
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));

            assertThatThrownBy(() -> challengeService.deleteChallenge(1L, 2L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Only the creator can delete this challenge");

            then(challengeRepository).should(never()).delete(any());
        }
    }

    @Nested
    @DisplayName("joinChallenge Tests")
    class JoinChallengeTests {

        @Test
        @DisplayName("Should join public challenge successfully")
        void shouldJoinPublicChallengeSuccessfully() {
            testChallenge.setPublic(true);
            testChallenge.setEndDate(LocalDateTime.now().plusDays(30)); // Not ended

            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(challengeRepository.isUserParticipating(1L, 2L)).willReturn(false);
            given(challengeRepository.save(any(Challenge.class))).willReturn(testChallenge);

            challengeService.joinChallenge(1L, 2L);

            assertThat(testChallenge.getParticipantIds()).contains(2L);
            then(challengeRepository).should().save(testChallenge);
        }

        @Test
        @DisplayName("Should throw exception when challenge has ended")
        void shouldThrowExceptionWhenChallengeEnded() {
            testChallenge.setEndDate(LocalDateTime.now().minusDays(1)); // Already ended
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));

            assertThatThrownBy(() -> challengeService.joinChallenge(1L, 2L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("This challenge has already ended");
        }

        @Test
        @DisplayName("Should throw exception when challenge is private")
        void shouldThrowExceptionWhenChallengeIsPrivate() {
            testChallenge.setPublic(false);
            testChallenge.setEndDate(LocalDateTime.now().plusDays(30));
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));

            assertThatThrownBy(() -> challengeService.joinChallenge(1L, 2L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("This challenge is private");
        }

        @Test
        @DisplayName("Should throw exception when already participating")
        void shouldThrowExceptionWhenAlreadyParticipating() {
            testChallenge.setPublic(true);
            testChallenge.setEndDate(LocalDateTime.now().plusDays(30));
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(challengeRepository.isUserParticipating(1L, 2L)).willReturn(true);

            assertThatThrownBy(() -> challengeService.joinChallenge(1L, 2L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("You are already participating in this challenge");
        }
    }

    @Nested
    @DisplayName("leaveChallenge Tests")
    class LeaveChallengeTests {

        @Test
        @DisplayName("Should leave challenge successfully")
        void shouldLeaveChallengeSuccessfully() {
            testChallenge.getParticipantIds().add(2L);
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(challengeRepository.isUserParticipating(1L, 2L)).willReturn(true);
            given(challengeRepository.save(any(Challenge.class))).willReturn(testChallenge);

            challengeService.leaveChallenge(1L, 2L);

            assertThat(testChallenge.getParticipantIds()).doesNotContain(2L);
            then(challengeRepository).should().save(testChallenge);
        }

        @Test
        @DisplayName("Should throw exception when creator tries to leave")
        void shouldThrowExceptionWhenCreatorTriesToLeave() {
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));

            assertThatThrownBy(() -> challengeService.leaveChallenge(1L, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Creator cannot leave the challenge");
        }

        @Test
        @DisplayName("Should throw exception when not participating")
        void shouldThrowExceptionWhenNotParticipating() {
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(challengeRepository.isUserParticipating(1L, 2L)).willReturn(false);

            assertThatThrownBy(() -> challengeService.leaveChallenge(1L, 2L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("You are not participating in this challenge");
        }
    }

    @Nested
    @DisplayName("getUserProgressInChallenge Tests")
    class GetUserProgressInChallengeTests {

        private void givenSummary(ActivitySummary summary) {
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(activityClient.summaryFor(eq(2L), eq("RUN"), any(), any())).willReturn(summary);
        }

        @Test
        @DisplayName("Should return distance progress")
        void shouldReturnDistanceProgress() {
            testChallenge.setChallengeType(ChallengeType.DISTANCE);
            givenSummary(new ActivitySummary(50.0, 0, 0, 0));

            assertThat(challengeService.getUserProgressInChallenge(1L, 2L)).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should return time progress in hours")
        void shouldReturnTimeProgressInHours() {
            testChallenge.setChallengeType(ChallengeType.TIME);
            givenSummary(new ActivitySummary(0, 7200, 0, 0)); // 2 hours in seconds

            assertThat(challengeService.getUserProgressInChallenge(1L, 2L)).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should return elevation progress")
        void shouldReturnElevationProgress() {
            testChallenge.setChallengeType(ChallengeType.ELEVATION);
            givenSummary(new ActivitySummary(0, 0, 1500.0, 0));

            assertThat(challengeService.getUserProgressInChallenge(1L, 2L)).isEqualTo(1500.0);
        }

        @Test
        @DisplayName("Should return activities count progress")
        void shouldReturnActivitiesCountProgress() {
            testChallenge.setChallengeType(ChallengeType.ACTIVITIES_COUNT);
            givenSummary(new ActivitySummary(0, 0, 0, 10));

            assertThat(challengeService.getUserProgressInChallenge(1L, 2L)).isEqualTo(10.0);
        }

        @Test
        @DisplayName("Should return zero when activity-service falls back")
        void shouldReturnZeroWhenNoActivities() {
            testChallenge.setChallengeType(ChallengeType.DISTANCE);
            givenSummary(ActivitySummary.EMPTY);

            assertThat(challengeService.getUserProgressInChallenge(1L, 2L)).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("searchChallenges Tests")
    class SearchChallengesTests {

        @Test
        @DisplayName("Should search challenges by text")
        void shouldSearchChallengesByText() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Challenge> searchResult = new PageImpl<>(List.of(testChallenge));
            given(challengeRepository.searchChallenges("Running", pageable)).willReturn(searchResult);

            Page<Challenge> result = challengeService.searchChallenges("Running", pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getLeaderboard Tests")
    class GetLeaderboardTests {

        @Test
        @DisplayName("Should build leaderboard sorted by progress with resolved names")
        void shouldBuildLeaderboardSortedByProgress() {
            Pageable pageable = PageRequest.of(0, 50);
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(challengeRepository.findParticipantIdsByChallengeId(1L, pageable))
                    .willReturn(new PageImpl<>(List.of(1L, 2L)));
            given(userBatchClient.usersByIds(anyCollection())).willReturn(Map.of(
                    1L, new UserSummary(1L, "John", "Doe", "John Doe", "avatar.png"),
                    2L, new UserSummary(2L, "Jane", "Smith", "Jane Smith", null)));
            given(activityClient.summaryFor(eq(1L), eq("RUN"), any(), any()))
                    .willReturn(new ActivitySummary(40.0, 0, 0, 0));
            given(activityClient.summaryFor(eq(2L), eq("RUN"), any(), any()))
                    .willReturn(new ActivitySummary(150.0, 0, 0, 0));

            List<Map<String, Object>> leaderboard = challengeService.getLeaderboard(1L, pageable);

            assertThat(leaderboard).hasSize(2);
            // Jane leads with 150km — capped at 100%
            assertThat(leaderboard.get(0).get("userId")).isEqualTo(2L);
            assertThat(leaderboard.get(0).get("userName")).isEqualTo("Jane Smith");
            assertThat(leaderboard.get(0).get("avatar")).isEqualTo("");
            assertThat(leaderboard.get(0).get("progress")).isEqualTo(150.0);
            assertThat(leaderboard.get(0).get("percentage")).isEqualTo(100.0);
            assertThat(leaderboard.get(1).get("userId")).isEqualTo(1L);
            assertThat(leaderboard.get(1).get("userName")).isEqualTo("John Doe");
            assertThat(leaderboard.get(1).get("avatar")).isEqualTo("avatar.png");
            assertThat(leaderboard.get(1).get("percentage")).isEqualTo(40.0);
        }

        @Test
        @DisplayName("Should render placeholder name when user lookup falls back")
        void shouldRenderPlaceholderNameWhenUserLookupFallsBack() {
            Pageable pageable = PageRequest.of(0, 50);
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(challengeRepository.findParticipantIdsByChallengeId(1L, pageable))
                    .willReturn(new PageImpl<>(List.of(7L)));
            given(userBatchClient.usersByIds(anyCollection())).willReturn(Map.of());
            given(activityClient.summaryFor(eq(7L), eq("RUN"), any(), any()))
                    .willReturn(new ActivitySummary(10.0, 0, 0, 0));

            List<Map<String, Object>> leaderboard = challengeService.getLeaderboard(1L, pageable);

            assertThat(leaderboard).hasSize(1);
            assertThat(leaderboard.getFirst().get("userName")).isEqualTo("User #7");
            assertThat(leaderboard.getFirst().get("avatar")).isEqualTo("");
        }
    }
}
