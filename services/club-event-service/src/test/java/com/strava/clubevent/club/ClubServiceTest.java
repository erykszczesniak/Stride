package com.strava.clubevent.club;

import com.strava.clubevent.common.ActivityType;
import com.strava.clubevent.exception.BadRequestException;
import com.strava.clubevent.exception.ForbiddenException;
import com.strava.clubevent.exception.NotFoundException;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClubService Unit Tests")
class ClubServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @InjectMocks
    private ClubService clubService;

    private Club testClub;

    @BeforeEach
    void setUp() {
        testClub = Club.builder()
                .id(1L)
                .name("Running Club")
                .description("A club for runners")
                .city("Warsaw")
                .country("Poland")
                .sportType(ActivityType.RUN)
                .isPrivate(false)
                .ownerId(1L)
                .build();
    }

    @Nested
    @DisplayName("getClubById Tests")
    class GetClubByIdTests {

        @Test
        @DisplayName("Should return club when found")
        void shouldReturnClubWhenFound() {
            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));

            Club result = clubService.getClubById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Running Club");
        }

        @Test
        @DisplayName("Should throw exception when club not found")
        void shouldThrowExceptionWhenClubNotFound() {
            given(clubRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> clubService.getClubById(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Club not found");
        }
    }

    @Nested
    @DisplayName("getAllPublicClubs Tests")
    class GetAllPublicClubsTests {

        @Test
        @DisplayName("Should return paginated public clubs")
        void shouldReturnPaginatedPublicClubs() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Club> clubPage = new PageImpl<>(List.of(testClub));
            given(clubRepository.findPublicClubsOrderByMembersCount(pageable)).willReturn(clubPage);

            Page<Club> result = clubService.getAllPublicClubs(pageable);

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
            given(clubRepository.findClubsByUserId(1L)).willReturn(List.of(testClub));

            List<Club> result = clubService.getClubsByUserId(1L);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("createClub Tests")
    class CreateClubTests {

        @Test
        @DisplayName("Should create club with owner as member and admin")
        void shouldCreateClubSuccessfully() {
            Club newClub = Club.builder()
                    .name("Cycling Club")
                    .description("A club for cyclists")
                    .sportType(ActivityType.RIDE)
                    .build();

            given(clubRepository.save(any(Club.class))).willAnswer(invocation -> {
                Club saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            Club result = clubService.createClub(newClub, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getOwnerId()).isEqualTo(1L);
            assertThat(result.getMemberIds()).contains(1L);
            assertThat(result.getAdminIds()).contains(1L);
            then(clubRepository).should().save(any(Club.class));
        }
    }

    @Nested
    @DisplayName("updateClub Tests")
    class UpdateClubTests {

        @Test
        @DisplayName("Should update club when user is owner")
        void shouldUpdateClubWhenUserIsOwner() {
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

            Club result = clubService.updateClub(1L, updatedData, 1L);

            assertThat(result.getName()).isEqualTo("Updated Club Name");
            assertThat(result.getDescription()).isEqualTo("Updated description");
            then(clubRepository).should().save(testClub);
        }

        @Test
        @DisplayName("Should update club when user is admin")
        void shouldUpdateClubWhenUserIsAdmin() {
            Club updatedData = Club.builder().name("Admin Updated").build();

            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));
            given(clubRepository.isUserAdmin(1L, 2L)).willReturn(true);
            given(clubRepository.save(any(Club.class))).willReturn(testClub);

            Club result = clubService.updateClub(1L, updatedData, 2L);

            assertThat(result).isNotNull();
            then(clubRepository).should().save(any(Club.class));
        }

        @Test
        @DisplayName("Should throw exception when user has no permission")
        void shouldThrowExceptionWhenUserHasNoPermission() {
            Club updatedData = Club.builder().name("Updated").build();

            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));
            given(clubRepository.isUserAdmin(1L, 3L)).willReturn(false);

            assertThatThrownBy(() -> clubService.updateClub(1L, updatedData, 3L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("You don't have permission to update this club");
        }
    }

    @Nested
    @DisplayName("deleteClub Tests")
    class DeleteClubTests {

        @Test
        @DisplayName("Should delete club when user is owner")
        void shouldDeleteClubWhenUserIsOwner() {
            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));

            clubService.deleteClub(1L, 1L);

            then(clubRepository).should().delete(testClub);
        }

        @Test
        @DisplayName("Should throw exception when user is not owner")
        void shouldThrowExceptionWhenUserIsNotOwner() {
            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));

            assertThatThrownBy(() -> clubService.deleteClub(1L, 2L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Only the owner can delete the club");

            then(clubRepository).should(never()).delete(any());
        }
    }

    @Nested
    @DisplayName("joinClub Tests")
    class JoinClubTests {

        @Test
        @DisplayName("Should join public club successfully")
        void shouldJoinPublicClubSuccessfully() {
            testClub.setPrivate(false);

            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));
            given(clubRepository.isUserMember(1L, 2L)).willReturn(false);
            given(clubRepository.save(any(Club.class))).willReturn(testClub);

            clubService.joinClub(1L, 2L);

            assertThat(testClub.getMemberIds()).contains(2L);
            then(clubRepository).should().save(testClub);
        }

        @Test
        @DisplayName("Should throw exception when club is private")
        void shouldThrowExceptionWhenClubIsPrivate() {
            testClub.setPrivate(true);
            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));

            assertThatThrownBy(() -> clubService.joinClub(1L, 2L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("This club is private. Request to join.");
        }

        @Test
        @DisplayName("Should throw exception when already a member")
        void shouldThrowExceptionWhenAlreadyMember() {
            testClub.setPrivate(false);
            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));
            given(clubRepository.isUserMember(1L, 2L)).willReturn(true);

            assertThatThrownBy(() -> clubService.joinClub(1L, 2L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("You are already a member of this club");
        }
    }

    @Nested
    @DisplayName("leaveClub Tests")
    class LeaveClubTests {

        @Test
        @DisplayName("Should leave club successfully")
        void shouldLeaveClubSuccessfully() {
            testClub.getMemberIds().add(2L);
            testClub.getAdminIds().add(2L);

            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));
            given(clubRepository.isUserMember(1L, 2L)).willReturn(true);
            given(clubRepository.save(any(Club.class))).willReturn(testClub);

            clubService.leaveClub(1L, 2L);

            assertThat(testClub.getMemberIds()).doesNotContain(2L);
            assertThat(testClub.getAdminIds()).doesNotContain(2L);
            then(clubRepository).should().save(testClub);
        }

        @Test
        @DisplayName("Should throw exception when owner tries to leave")
        void shouldThrowExceptionWhenOwnerTriesToLeave() {
            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));

            assertThatThrownBy(() -> clubService.leaveClub(1L, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Owner cannot leave the club. Transfer ownership first.");
        }

        @Test
        @DisplayName("Should throw exception when not a member")
        void shouldThrowExceptionWhenNotMember() {
            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));
            given(clubRepository.isUserMember(1L, 2L)).willReturn(false);

            assertThatThrownBy(() -> clubService.leaveClub(1L, 2L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("You are not a member of this club");
        }
    }

    @Nested
    @DisplayName("getClubMemberIds Tests")
    class GetClubMemberIdsTests {

        @Test
        @DisplayName("Should return club member ids")
        void shouldReturnClubMemberIds() {
            testClub.getMemberIds().add(1L);
            testClub.getMemberIds().add(2L);
            given(clubRepository.findById(1L)).willReturn(Optional.of(testClub));

            List<Long> members = clubService.getClubMemberIds(1L);

            assertThat(members).containsExactlyInAnyOrder(1L, 2L);
        }
    }

    @Nested
    @DisplayName("isUserMember Tests")
    class IsUserMemberTests {

        @Test
        @DisplayName("Should return true when user is member")
        void shouldReturnTrueWhenUserIsMember() {
            given(clubRepository.isUserMember(1L, 2L)).willReturn(true);

            assertThat(clubService.isUserMember(1L, 2L)).isTrue();
        }

        @Test
        @DisplayName("Should return false when user is not member")
        void shouldReturnFalseWhenUserIsNotMember() {
            given(clubRepository.isUserMember(1L, 3L)).willReturn(false);

            assertThat(clubService.isUserMember(1L, 3L)).isFalse();
        }
    }

    @Nested
    @DisplayName("searchClubs Tests")
    class SearchClubsTests {

        @Test
        @DisplayName("Should search clubs by text")
        void shouldSearchClubsByText() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Club> searchResult = new PageImpl<>(List.of(testClub));
            given(clubRepository.searchClubs("Running", pageable)).willReturn(searchResult);

            Page<Club> result = clubService.searchClubs("Running", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getName()).contains("Running");
        }
    }

    @Nested
    @DisplayName("stats Tests")
    class StatsTests {

        @Test
        @DisplayName("Should count public clubs and members")
        void shouldCountPublicClubsAndMembers() {
            given(clubRepository.countPublicClubs()).willReturn(10L);
            given(clubRepository.sumAllMembers()).willReturn(100L);

            assertThat(clubService.countPublicClubs()).isEqualTo(10L);
            assertThat(clubService.countTotalMembers()).isEqualTo(100L);
        }
    }
}
