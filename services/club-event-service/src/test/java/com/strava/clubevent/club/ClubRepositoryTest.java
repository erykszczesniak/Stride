package com.strava.clubevent.club;

import com.strava.clubevent.common.ActivityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against H2 with the real Flyway chain (V1 + V2) — validates the
 * migration scripts, the member/admin element collections and the
 * SIZE()-based ordering/aggregation JPQL ported from the monolith.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("ClubRepository Integration Tests")
class ClubRepositoryTest {

    @Autowired
    private ClubRepository clubRepository;

    private Club bigClub;

    private Club club(String name, String city, ActivityType sport, boolean isPrivate,
                      Long ownerId, Set<Long> members, Set<Long> admins) {
        Club c = Club.builder()
                .name(name)
                .city(city)
                .sportType(sport)
                .isPrivate(isPrivate)
                .ownerId(ownerId)
                .build();
        c.getMemberIds().addAll(members);
        c.getAdminIds().addAll(admins);
        return clubRepository.save(c);
    }

    @BeforeEach
    void setUp() {
        clubRepository.deleteAll();
        bigClub = club("Warsaw Runners", "Warsaw", ActivityType.RUN, false, 1L, Set.of(1L, 2L, 3L), Set.of(1L));
        club("Krakow Cyclists", "Krakow", ActivityType.RIDE, false, 2L, Set.of(2L), Set.of(2L));
        club("Secret Swimmers", "Warsaw", ActivityType.SWIM, true, 3L, Set.of(3L, 4L), Set.of(3L));
    }

    @Test
    @DisplayName("Should return public clubs ordered by member count")
    void shouldReturnPublicClubsOrderedByMembersCount() {
        Page<Club> result = clubRepository.findPublicClubsOrderByMembersCount(PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Club::getName)
                .containsExactly("Warsaw Runners", "Krakow Cyclists");
    }

    @Test
    @DisplayName("Should find clubs of a user ordered by name")
    void shouldFindClubsByUserId() {
        List<Club> result = clubRepository.findClubsByUserId(2L);

        assertThat(result).extracting(Club::getName)
                .containsExactly("Krakow Cyclists", "Warsaw Runners");
    }

    @Test
    @DisplayName("Should detect membership and admin role")
    void shouldDetectMembershipAndAdminRole() {
        assertThat(clubRepository.isUserMember(bigClub.getId(), 2L)).isTrue();
        assertThat(clubRepository.isUserMember(bigClub.getId(), 99L)).isFalse();
        assertThat(clubRepository.isUserAdmin(bigClub.getId(), 1L)).isTrue();
        assertThat(clubRepository.isUserAdmin(bigClub.getId(), 2L)).isFalse();
    }

    @Test
    @DisplayName("Should search clubs by name or city, case-insensitive")
    void shouldSearchClubsByNameOrCity() {
        Page<Club> byName = clubRepository.searchClubs("runners", PageRequest.of(0, 20));
        Page<Club> byCity = clubRepository.searchClubs("warsaw", PageRequest.of(0, 20));

        assertThat(byName.getContent()).extracting(Club::getName).containsExactly("Warsaw Runners");
        assertThat(byCity.getContent()).extracting(Club::getName)
                .containsExactlyInAnyOrder("Warsaw Runners", "Secret Swimmers");
    }

    @Test
    @DisplayName("Should filter public clubs by sport type")
    void shouldFilterPublicClubsBySportType() {
        Page<Club> result = clubRepository.findPublicClubsBySportType(ActivityType.RIDE, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Club::getName).containsExactly("Krakow Cyclists");
    }

    @Test
    @DisplayName("Should count public clubs and sum their members")
    void shouldCountPublicClubsAndSumMembers() {
        assertThat(clubRepository.countPublicClubs()).isEqualTo(2);
        assertThat(clubRepository.sumAllMembers()).isEqualTo(4); // 3 runners + 1 cyclist
    }
}
