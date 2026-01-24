package com.strava.backend.repository;

import com.strava.backend.domain.Club;
import com.strava.backend.domain.User;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.AuthProvider;
import com.strava.backend.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {"spring.flyway.enabled=false"})
@DisplayName("ClubRepository Integration Tests")
class ClubRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ClubRepository clubRepository;

    private User owner;
    private User member;
    private User admin;
    private Club publicClub;
    private Club privateClub;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .email("owner@example.com")
                .firstName("Owner")
                .lastName("User")
                .password("password123")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .active(true)
                .build();
        entityManager.persist(owner);

        member = User.builder()
                .email("member@example.com")
                .firstName("Member")
                .lastName("User")
                .password("password123")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .active(true)
                .build();
        entityManager.persist(member);

        admin = User.builder()
                .email("admin@example.com")
                .firstName("Admin")
                .lastName("User")
                .password("password123")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .active(true)
                .build();
        entityManager.persist(admin);

        publicClub = Club.builder()
                .name("Running Club Warsaw")
                .description("A club for runners in Warsaw")
                .city("Warsaw")
                .country("Poland")
                .sportType(ActivityType.RUN)
                .isPrivate(false)
                .owner(owner)
                .build();
        publicClub.getMembers().add(owner);
        publicClub.getMembers().add(member);
        publicClub.getAdmins().add(owner);
        publicClub.getAdmins().add(admin);
        entityManager.persist(publicClub);

        privateClub = Club.builder()
                .name("Elite Cyclists")
                .description("Private cycling club")
                .city("Krakow")
                .country("Poland")
                .sportType(ActivityType.RIDE)
                .isPrivate(true)
                .owner(owner)
                .build();
        privateClub.getMembers().add(owner);
        privateClub.getAdmins().add(owner);
        entityManager.persist(privateClub);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findPublicClubsOrderByMembersCount Tests")
    class FindPublicClubsTests {

        @Test
        @DisplayName("Should return only public clubs ordered by member count")
        void shouldReturnOnlyPublicClubs() {
            Page<Club> clubs = clubRepository.findPublicClubsOrderByMembersCount(PageRequest.of(0, 10));

            assertThat(clubs.getContent()).hasSize(1);
            assertThat(clubs.getContent().getFirst().getName()).isEqualTo("Running Club Warsaw");
            assertThat(clubs.getContent().getFirst().isPrivate()).isFalse();
        }

        @Test
        @DisplayName("Should order clubs by member count descending")
        void shouldOrderByMemberCountDescending() {
            // Create another public club with more members
            Club bigClub = Club.builder()
                    .name("Big Running Club")
                    .description("A larger club")
                    .city("Gdansk")
                    .country("Poland")
                    .sportType(ActivityType.RUN)
                    .isPrivate(false)
                    .owner(owner)
                    .build();
            bigClub.getMembers().add(owner);
            bigClub.getMembers().add(member);
            bigClub.getMembers().add(admin);
            entityManager.persist(bigClub);
            entityManager.flush();
            entityManager.clear();

            Page<Club> clubs = clubRepository.findPublicClubsOrderByMembersCount(PageRequest.of(0, 10));

            assertThat(clubs.getContent()).hasSize(2);
            assertThat(clubs.getContent().get(0).getName()).isEqualTo("Big Running Club");
        }
    }

    @Nested
    @DisplayName("findClubsByUserId Tests")
    class FindClubsByUserIdTests {

        @Test
        @DisplayName("Should return clubs where user is a member")
        void shouldReturnClubsWhereUserIsMember() {
            List<Club> clubs = clubRepository.findClubsByUserId(member.getId());

            assertThat(clubs).hasSize(1);
            assertThat(clubs.getFirst().getName()).isEqualTo("Running Club Warsaw");
        }

        @Test
        @DisplayName("Should return all clubs for owner")
        void shouldReturnAllClubsForOwner() {
            List<Club> clubs = clubRepository.findClubsByUserId(owner.getId());

            assertThat(clubs).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list for non-member")
        void shouldReturnEmptyListForNonMember() {
            User nonMember = User.builder()
                    .email("nonmember@example.com")
                    .firstName("Non")
                    .lastName("Member")
                    .password("password123")
                    .authProvider(AuthProvider.LOCAL)
                    .role(Role.USER)
                    .active(true)
                    .build();
            entityManager.persist(nonMember);
            entityManager.flush();

            List<Club> clubs = clubRepository.findClubsByUserId(nonMember.getId());

            assertThat(clubs).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByOwnerId Tests")
    class FindByOwnerIdTests {

        @Test
        @DisplayName("Should return clubs owned by user")
        void shouldReturnClubsOwnedByUser() {
            List<Club> clubs = clubRepository.findByOwnerId(owner.getId());

            assertThat(clubs).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list for non-owner")
        void shouldReturnEmptyListForNonOwner() {
            List<Club> clubs = clubRepository.findByOwnerId(member.getId());

            assertThat(clubs).isEmpty();
        }
    }

    @Nested
    @DisplayName("isUserMember Tests")
    class IsUserMemberTests {

        @Test
        @DisplayName("Should return true when user is member")
        void shouldReturnTrueWhenUserIsMember() {
            boolean isMember = clubRepository.isUserMember(publicClub.getId(), member.getId());

            assertThat(isMember).isTrue();
        }

        @Test
        @DisplayName("Should return false when user is not member")
        void shouldReturnFalseWhenUserIsNotMember() {
            boolean isMember = clubRepository.isUserMember(publicClub.getId(), admin.getId());

            assertThat(isMember).isFalse();
        }
    }

    @Nested
    @DisplayName("isUserAdmin Tests")
    class IsUserAdminTests {

        @Test
        @DisplayName("Should return true when user is admin")
        void shouldReturnTrueWhenUserIsAdmin() {
            boolean isAdmin = clubRepository.isUserAdmin(publicClub.getId(), admin.getId());

            assertThat(isAdmin).isTrue();
        }

        @Test
        @DisplayName("Should return false when user is not admin")
        void shouldReturnFalseWhenUserIsNotAdmin() {
            boolean isAdmin = clubRepository.isUserAdmin(publicClub.getId(), member.getId());

            assertThat(isAdmin).isFalse();
        }
    }

    @Nested
    @DisplayName("searchClubs Tests")
    class SearchClubsTests {

        @Test
        @DisplayName("Should find clubs by name")
        void shouldFindClubsByName() {
            Page<Club> result = clubRepository.searchClubs("Running", PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getName()).contains("Running");
        }

        @Test
        @DisplayName("Should find clubs by city")
        void shouldFindClubsByCity() {
            Page<Club> result = clubRepository.searchClubs("Warsaw", PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getCity()).isEqualTo("Warsaw");
        }

        @Test
        @DisplayName("Should be case insensitive")
        void shouldBeCaseInsensitive() {
            Page<Club> result = clubRepository.searchClubs("RUNNING", PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty when no matches")
        void shouldReturnEmptyWhenNoMatches() {
            Page<Club> result = clubRepository.searchClubs("NonExistent", PageRequest.of(0, 10));

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findPublicClubsByCity Tests")
    class FindPublicClubsByCityTests {

        @Test
        @DisplayName("Should return public clubs in city")
        void shouldReturnPublicClubsInCity() {
            List<Club> clubs = clubRepository.findPublicClubsByCity("Warsaw");

            assertThat(clubs).hasSize(1);
            assertThat(clubs.getFirst().getName()).isEqualTo("Running Club Warsaw");
        }

        @Test
        @DisplayName("Should not return private clubs")
        void shouldNotReturnPrivateClubs() {
            List<Club> clubs = clubRepository.findPublicClubsByCity("Krakow");

            assertThat(clubs).isEmpty();
        }
    }
}
