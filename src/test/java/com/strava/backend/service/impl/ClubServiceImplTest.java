package com.strava.backend.service.impl;

import com.strava.backend.domain.Club;
import com.strava.backend.domain.User;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.ClubRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClubServiceImpl Unit Tests")
class ClubServiceImplTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClubServiceImpl clubService;

    private User owner;
    private User member;
    private Club testClub;

    @BeforeEach
    void setUp() {
        owner = TestDataFactory.createUser(1L, "owner@example.com", "Owner", "User");
        member = TestDataFactory.createUser(2L, "member@example.com", "Member", "User");
        testClub = TestDataFactory.createClub(1L, owner);
    }

    @Nested
    @DisplayName("getClubById Tests")
    class GetClubByIdTests {

        @Test
        @DisplayName("Should return club when found")
        void shouldReturnClubWhenFound() {
            // Given
            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));

            // When
            Club result = clubService.getClubById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Running Club");
        }

        @Test
        @DisplayName("Should throw exception when club not found")
        void shouldThrowExceptionWhenClubNotFound() {
            // Given
            given(clubRepository.findById(999L)).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> clubService.getClubById(999L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Club not found")
                    .extracting("status")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getAllPublicClubs Tests")
    class GetAllPublicClubsTests {

        @Test
        @DisplayName("Should return paginated public clubs")
        void shouldReturnPaginatedPublicClubs() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Club> clubPage = new PageImpl<>(List.of(testClub));
            given(clubRepository.findPublicClubsOrderByMembersCount(pageable)).willReturn(clubPage);

            // When
            Page<Club> result = clubService.getAllPublicClubs(pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getName()).isEqualTo("Running Club");
        }
    }

    @Nested
    @DisplayName("getClubsByUserId Tests")
    class GetClubsByUserIdTests {

        @Test
        @DisplayName("Should return clubs for user")
        void shouldReturnClubsForUser() {
            // Given
            given(clubRepository.findClubsByUserId(1L)).willReturn(List.of(testClub));

            // When
            List<Club> result = clubService.getClubsByUserId(1L);

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("createClub Tests")
    class CreateClubTests {

        @Test
        @DisplayName("Should create club successfully")
        void shouldCreateClubSuccessfully() {
            // Given
            Club newClub = Club.builder()
                    .name("Cycling Club")
                    .description("A club for cyclists")
                    .sportType(ActivityType.RIDE)
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(owner));
            given(clubRepository.save(any(Club.class))).willAnswer(invocation -> {
                Club saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // When
            Club result = clubService.createClub(newClub, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOwner()).isEqualTo(owner);
            assertThat(result.getMembers()).contains(owner);
            assertThat(result.getAdmins()).contains(owner);

            then(clubRepository).should().save(any(Club.class));
        }

        @Test
        @DisplayName("Should throw exception when owner not found")
        void shouldThrowExceptionWhenOwnerNotFound() {
            // Given
            Club newClub = Club.builder().name("Test Club").build();
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> clubService.createClub(newClub, 999L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("User not found");

            then(clubRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateClub Tests")
    class UpdateClubTests {

        @Test
        @DisplayName("Should update club when user is owner")
        void shouldUpdateClubWhenUserIsOwner() {
            // Given
            Club updatedData = Club.builder()
                    .name("Updated Club Name")
                    .description("Updated description")
                    .city("New City")
                    .country("New Country")
                    .sportType(ActivityType.SWIM)
                    .isPrivate(true)
                    .build();

            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));
            given(clubRepository.save(any(Club.class))).willReturn(testClub);

            // When
            Club result = clubService.updateClub(1L, updatedData, 1L);

            // Then
            assertThat(result.getName()).isEqualTo("Updated Club Name");
            assertThat(result.getDescription()).isEqualTo("Updated description");
            then(clubRepository).should().save(testClub);
        }

        @Test
        @DisplayName("Should update club when user is admin")
        void shouldUpdateClubWhenUserIsAdmin() {
            // Given
            Club updatedData = Club.builder().name("Admin Updated").build();

            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));
            given(clubRepository.isUserAdmin(1L, 2L)).willReturn(true);
            given(clubRepository.save(any(Club.class))).willReturn(testClub);

            // When
            Club result = clubService.updateClub(1L, updatedData, 2L);

            // Then
            assertThat(result).isNotNull();
            then(clubRepository).should().save(any(Club.class));
        }

        @Test
        @DisplayName("Should throw exception when user has no permission")
        void shouldThrowExceptionWhenUserHasNoPermission() {
            // Given
            Club updatedData = Club.builder().name("Updated").build();

            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));
            given(clubRepository.isUserAdmin(1L, 3L)).willReturn(false);

            // When/Then
            assertThatThrownBy(() -> clubService.updateClub(1L, updatedData, 3L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("You don't have permission to update this club")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("deleteClub Tests")
    class DeleteClubTests {

        @Test
        @DisplayName("Should delete club when user is owner")
        void shouldDeleteClubWhenUserIsOwner() {
            // Given
            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));

            // When
            clubService.deleteClub(1L, 1L);

            // Then
            then(clubRepository).should().delete(testClub);
        }

        @Test
        @DisplayName("Should throw exception when user is not owner")
        void shouldThrowExceptionWhenUserIsNotOwner() {
            // Given
            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));

            // When/Then
            assertThatThrownBy(() -> clubService.deleteClub(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Only the owner can delete the club")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            then(clubRepository).should(never()).delete(any());
        }
    }

    @Nested
    @DisplayName("joinClub Tests")
    class JoinClubTests {

        @Test
        @DisplayName("Should join public club successfully")
        void shouldJoinPublicClubSuccessfully() {
            // Given
            testClub.setPrivate(false);

            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));
            given(userRepository.findById(2L)).willReturn(Optional.of(member));
            given(clubRepository.isUserMember(1L, 2L)).willReturn(false);
            given(clubRepository.save(any(Club.class))).willReturn(testClub);

            // When
            clubService.joinClub(1L, 2L);

            // Then
            assertThat(testClub.getMembers()).contains(member);
            then(clubRepository).should().save(testClub);
        }

        @Test
        @DisplayName("Should throw exception when club is private")
        void shouldThrowExceptionWhenClubIsPrivate() {
            // Given
            testClub.setPrivate(true);

            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));
            given(userRepository.findById(2L)).willReturn(Optional.of(member));

            // When/Then
            assertThatThrownBy(() -> clubService.joinClub(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("This club is private. Request to join.")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Should throw exception when already a member")
        void shouldThrowExceptionWhenAlreadyMember() {
            // Given
            testClub.setPrivate(false);

            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));
            given(userRepository.findById(2L)).willReturn(Optional.of(member));
            given(clubRepository.isUserMember(1L, 2L)).willReturn(true);

            // When/Then
            assertThatThrownBy(() -> clubService.joinClub(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("You are already a member of this club")
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("leaveClub Tests")
    class LeaveClubTests {

        @Test
        @DisplayName("Should leave club successfully")
        void shouldLeaveClubSuccessfully() {
            // Given
            testClub.getMembers().add(member);

            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));
            given(userRepository.findById(2L)).willReturn(Optional.of(member));
            given(clubRepository.isUserMember(1L, 2L)).willReturn(true);
            given(clubRepository.save(any(Club.class))).willReturn(testClub);

            // When
            clubService.leaveClub(1L, 2L);

            // Then
            assertThat(testClub.getMembers()).doesNotContain(member);
            then(clubRepository).should().save(testClub);
        }

        @Test
        @DisplayName("Should throw exception when owner tries to leave")
        void shouldThrowExceptionWhenOwnerTriesToLeave() {
            // Given
            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));

            // When/Then
            assertThatThrownBy(() -> clubService.leaveClub(1L, 1L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Owner cannot leave the club. Transfer ownership first.")
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should throw exception when not a member")
        void shouldThrowExceptionWhenNotMember() {
            // Given
            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));
            given(userRepository.findById(2L)).willReturn(Optional.of(member));
            given(clubRepository.isUserMember(1L, 2L)).willReturn(false);

            // When/Then
            assertThatThrownBy(() -> clubService.leaveClub(1L, 2L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("You are not a member of this club")
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("getClubMembers Tests")
    class GetClubMembersTests {

        @Test
        @DisplayName("Should return club members")
        void shouldReturnClubMembers() {
            // Given
            testClub.getMembers().add(owner);
            testClub.getMembers().add(member);
            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));

            // When
            List<?> members = clubService.getClubMembers(1L);

            // Then
            assertThat(members).hasSize(2);
        }
    }

    @Nested
    @DisplayName("isUserMember Tests")
    class IsUserMemberTests {

        @Test
        @DisplayName("Should return true when user is member")
        void shouldReturnTrueWhenUserIsMember() {
            // Given
            given(clubRepository.isUserMember(1L, 2L)).willReturn(true);

            // When
            boolean result = clubService.isUserMember(1L, 2L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when user is not member")
        void shouldReturnFalseWhenUserIsNotMember() {
            // Given
            given(clubRepository.isUserMember(1L, 3L)).willReturn(false);

            // When
            boolean result = clubService.isUserMember(1L, 3L);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("searchClubs Tests")
    class SearchClubsTests {

        @Test
        @DisplayName("Should search clubs by text")
        void shouldSearchClubsByText() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Club> searchResult = new PageImpl<>(List.of(testClub));
            given(clubRepository.searchClubs("Running", pageable)).willReturn(searchResult);

            // When
            Page<Club> result = clubService.searchClubs("Running", pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getName()).contains("Running");
        }
    }
}
