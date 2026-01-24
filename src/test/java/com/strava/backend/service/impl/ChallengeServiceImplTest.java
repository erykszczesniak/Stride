package com.strava.backend.service.impl;

import com.strava.backend.domain.Challenge;
import com.strava.backend.domain.User;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.ChallengeType;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.ChallengeRepository;
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

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengeServiceImpl Unit Tests")
class ChallengeServiceImplTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private ChallengeServiceImpl challengeService;

    private User creator;
    private User participant;
    private Challenge testChallenge;

    @BeforeEach
    void setUp() {
        creator = TestDataFactory.createUser(1L, "creator@example.com", "Creator", "User");
        participant = TestDataFactory.createUser(2L, "participant@example.com", "Participant", "User");
        testChallenge = TestDataFactory.createChallenge(1L, creator);
    }

    @Nested
    @DisplayName("getChallengeById Tests")
    class GetChallengeByIdTests {

        @Test
        @DisplayName("Should return challenge when found")
        void shouldReturnChallengeWhenFound() {
            // Given
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));

            // When
            Challenge result = challengeService.getChallengeById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Monthly Running Challenge");
        }

        @Test
        @DisplayName("Should throw exception when challenge not found")
        void shouldThrowExceptionWhenChallengeNotFound() {
            // Given
            given(challengeRepository.findById(999L)).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> challengeService.getChallengeById(999L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Challenge not found")
                    .extracting("status")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getActiveChallenges Tests")
    class GetActiveChallengesTests {

        @Test
        @DisplayName("Should return paginated active challenges")
        void shouldReturnPaginatedActiveChallenges() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Challenge> challengePage = new PageImpl<>(List.of(testChallenge));
            given(challengeRepository.findActivePublicChallenges(any(LocalDateTime.class), eq(pageable)))
                    .willReturn(challengePage);

            // When
            Page<Challenge> result = challengeService.getActiveChallenges(pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getUpcomingChallenges Tests")
    class GetUpcomingChallengesTests {

        @Test
        @DisplayName("Should return upcoming challenges")
        void shouldReturnUpcomingChallenges() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Challenge> challengePage = new PageImpl<>(List.of(testChallenge));
            given(challengeRepository.findUpcomingPublicChallenges(any(LocalDateTime.class), eq(pageable)))
                    .willReturn(challengePage);

            // When
            Page<Challenge> result = challengeService.getUpcomingChallenges(pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("createChallenge Tests")
    class CreateChallengeTests {

        @Test
        @DisplayName("Should create challenge successfully")
        void shouldCreateChallengeSuccessfully() {
            // Given
            Challenge newChallenge = Challenge.builder()
                    .name("New Challenge")
                    .challengeType(ChallengeType.DISTANCE)
                    .activityType(ActivityType.RUN)
                    .targetValue(50.0)
                    .startDate(LocalDateTime.now().plusDays(1))
                    .endDate(LocalDateTime.now().plusDays(30))
                    .isPublic(true)
                    .participants(new HashSet<>())
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(creator));
            given(challengeRepository.save(any(Challenge.class))).willAnswer(invocation -> {
                Challenge saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // When
            Challenge result = challengeService.createChallenge(newChallenge, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCreator()).isEqualTo(creator);
            assertThat(result.getParticipants()).contains(creator);

            then(challengeRepository).should().save(any(Challenge.class));
        }

        @Test
        @DisplayName("Should throw exception when creator not found")
        void shouldThrowExceptionWhenCreatorNotFound() {
            // Given
            Challenge newChallenge = Challenge.builder().name("Test").participants(new HashSet<>()).build();
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> challengeService.createChallenge(newChallenge, 999L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("User not found");
        }
    }

    @Nested
    @DisplayName("updateChallenge Tests")
    class UpdateChallengeTests {

        @Test
        @DisplayName("Should update challenge when user is creator and challenge not started")
        void shouldUpdateChallengeWhenUserIsCreator() {
            // Given
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

            // When
            Challenge result = challengeService.updateChallenge(1L, updatedData, 1L);

            // Then
            assertThat(result.getName()).isEqualTo("Updated Challenge");
            then(challengeRepository).should().save(testChallenge);
        }

        @Test
        @DisplayName("Should throw exception when user is not creator")
        void shouldThrowExceptionWhenUserIsNotCreator() {
            // Given
            Challenge updatedData = Challenge.builder().name("Updated").build();
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));

            // When/Then
            assertThatThrownBy(() -> challengeService.updateChallenge(1L, updatedData, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Only the creator can update this challenge")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Should throw exception when challenge has already started")
        void shouldThrowExceptionWhenChallengeStarted() {
            // Given
            testChallenge.setStartDate(LocalDateTime.now().minusDays(1)); // Already started
            Challenge updatedData = Challenge.builder().name("Updated").build();
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));

            // When/Then
            assertThatThrownBy(() -> challengeService.updateChallenge(1L, updatedData, 1L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Cannot update a challenge that has already started")
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("deleteChallenge Tests")
    class DeleteChallengeTests {

        @Test
        @DisplayName("Should delete challenge when user is creator")
        void shouldDeleteChallengeWhenUserIsCreator() {
            // Given
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));

            // When
            challengeService.deleteChallenge(1L, 1L);

            // Then
            then(challengeRepository).should().delete(testChallenge);
        }

        @Test
        @DisplayName("Should throw exception when user is not creator")
        void shouldThrowExceptionWhenUserIsNotCreator() {
            // Given
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));

            // When/Then
            assertThatThrownBy(() -> challengeService.deleteChallenge(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Only the creator can delete this challenge")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            then(challengeRepository).should(never()).delete(any());
        }
    }

    @Nested
    @DisplayName("joinChallenge Tests")
    class JoinChallengeTests {

        @Test
        @DisplayName("Should join public challenge successfully")
        void shouldJoinPublicChallengeSuccessfully() {
            // Given
            testChallenge.setPublic(true);
            testChallenge.setEndDate(LocalDateTime.now().plusDays(30)); // Not ended

            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(userRepository.findById(2L)).willReturn(Optional.of(participant));
            given(challengeRepository.isUserParticipating(1L, 2L)).willReturn(false);
            given(challengeRepository.save(any(Challenge.class))).willReturn(testChallenge);

            // When
            challengeService.joinChallenge(1L, 2L);

            // Then
            assertThat(testChallenge.getParticipants()).contains(participant);
            then(challengeRepository).should().save(testChallenge);
        }

        @Test
        @DisplayName("Should throw exception when challenge has ended")
        void shouldThrowExceptionWhenChallengeEnded() {
            // Given
            testChallenge.setEndDate(LocalDateTime.now().minusDays(1)); // Already ended

            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(userRepository.findById(2L)).willReturn(Optional.of(participant));

            // When/Then
            assertThatThrownBy(() -> challengeService.joinChallenge(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("This challenge has already ended")
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should throw exception when challenge is private")
        void shouldThrowExceptionWhenChallengeIsPrivate() {
            // Given
            testChallenge.setPublic(false);
            testChallenge.setEndDate(LocalDateTime.now().plusDays(30));

            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(userRepository.findById(2L)).willReturn(Optional.of(participant));

            // When/Then
            assertThatThrownBy(() -> challengeService.joinChallenge(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("This challenge is private")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Should throw exception when already participating")
        void shouldThrowExceptionWhenAlreadyParticipating() {
            // Given
            testChallenge.setPublic(true);
            testChallenge.setEndDate(LocalDateTime.now().plusDays(30));

            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(userRepository.findById(2L)).willReturn(Optional.of(participant));
            given(challengeRepository.isUserParticipating(1L, 2L)).willReturn(true);

            // When/Then
            assertThatThrownBy(() -> challengeService.joinChallenge(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("You are already participating in this challenge")
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("leaveChallenge Tests")
    class LeaveChallengeTests {

        @Test
        @DisplayName("Should leave challenge successfully")
        void shouldLeaveChallengeSuccessfully() {
            // Given
            testChallenge.getParticipants().add(participant);

            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(userRepository.findById(2L)).willReturn(Optional.of(participant));
            given(challengeRepository.isUserParticipating(1L, 2L)).willReturn(true);
            given(challengeRepository.save(any(Challenge.class))).willReturn(testChallenge);

            // When
            challengeService.leaveChallenge(1L, 2L);

            // Then
            assertThat(testChallenge.getParticipants()).doesNotContain(participant);
            then(challengeRepository).should().save(testChallenge);
        }

        @Test
        @DisplayName("Should throw exception when creator tries to leave")
        void shouldThrowExceptionWhenCreatorTriesToLeave() {
            // Given
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(userRepository.findById(1L)).willReturn(Optional.of(creator));

            // When/Then
            assertThatThrownBy(() -> challengeService.leaveChallenge(1L, 1L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Creator cannot leave the challenge")
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should throw exception when not participating")
        void shouldThrowExceptionWhenNotParticipating() {
            // Given
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(userRepository.findById(2L)).willReturn(Optional.of(participant));
            given(challengeRepository.isUserParticipating(1L, 2L)).willReturn(false);

            // When/Then
            assertThatThrownBy(() -> challengeService.leaveChallenge(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("You are not participating in this challenge")
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("getUserProgressInChallenge Tests")
    class GetUserProgressInChallengeTests {

        @Test
        @DisplayName("Should return distance progress")
        void shouldReturnDistanceProgress() {
            // Given
            testChallenge.setChallengeType(ChallengeType.DISTANCE);
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(activityRepository.sumDistanceForChallenge(eq(2L), eq(ActivityType.RUN), any(), any()))
                    .willReturn(50.0);

            // When
            Double progress = challengeService.getUserProgressInChallenge(1L, 2L);

            // Then
            assertThat(progress).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should return time progress in hours")
        void shouldReturnTimeProgressInHours() {
            // Given
            testChallenge.setChallengeType(ChallengeType.TIME);
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(activityRepository.sumDurationForChallenge(eq(2L), eq(ActivityType.RUN), any(), any()))
                    .willReturn(7200.0); // 2 hours in seconds

            // When
            Double progress = challengeService.getUserProgressInChallenge(1L, 2L);

            // Then
            assertThat(progress).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should return elevation progress")
        void shouldReturnElevationProgress() {
            // Given
            testChallenge.setChallengeType(ChallengeType.ELEVATION);
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(activityRepository.sumElevationForChallenge(eq(2L), eq(ActivityType.RUN), any(), any()))
                    .willReturn(1500.0);

            // When
            Double progress = challengeService.getUserProgressInChallenge(1L, 2L);

            // Then
            assertThat(progress).isEqualTo(1500.0);
        }

        @Test
        @DisplayName("Should return activities count progress")
        void shouldReturnActivitiesCountProgress() {
            // Given
            testChallenge.setChallengeType(ChallengeType.ACTIVITIES_COUNT);
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(activityRepository.countActivitiesForChallenge(eq(2L), eq(ActivityType.RUN), any(), any()))
                    .willReturn(10.0);

            // When
            Double progress = challengeService.getUserProgressInChallenge(1L, 2L);

            // Then
            assertThat(progress).isEqualTo(10.0);
        }

        @Test
        @DisplayName("Should return zero when no activities")
        void shouldReturnZeroWhenNoActivities() {
            // Given
            testChallenge.setChallengeType(ChallengeType.DISTANCE);
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(activityRepository.sumDistanceForChallenge(eq(2L), eq(ActivityType.RUN), any(), any()))
                    .willReturn(null);

            // When
            Double progress = challengeService.getUserProgressInChallenge(1L, 2L);

            // Then
            assertThat(progress).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("searchChallenges Tests")
    class SearchChallengesTests {

        @Test
        @DisplayName("Should search challenges by text")
        void shouldSearchChallengesByText() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Challenge> searchResult = new PageImpl<>(List.of(testChallenge));
            given(challengeRepository.searchChallenges("Running", pageable)).willReturn(searchResult);

            // When
            Page<Challenge> result = challengeService.searchChallenges("Running", pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }
    }
}
