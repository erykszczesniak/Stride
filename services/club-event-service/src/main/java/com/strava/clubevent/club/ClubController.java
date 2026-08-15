package com.strava.clubevent.club;

import com.strava.clubevent.client.UserBatchClient;
import com.strava.clubevent.client.UserSummary;
import com.strava.clubevent.common.ActivityType;
import com.strava.clubevent.common.BaseUserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Public contract identical to the monolith's ClubController (/api/v1/clubs —
 * plain JSON array list envelopes, PUT updates, POST join/leave, member lists
 * as BaseUserResponse). Owner/member names are hydrated from user-service in
 * one batch call per request.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/clubs")
public class ClubController {

    private final ClubService clubService;
    private final UserBatchClient userBatchClient;

    @GetMapping
    public ResponseEntity<List<ClubResponse>> getAllClubs(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) ActivityType sportType,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Club> clubs;
        if (sportType != null) {
            clubs = clubService.getClubsBySportType(sportType, pageable);
        } else {
            clubs = clubService.getAllPublicClubs(pageable);
        }
        return ResponseEntity.ok(mapAll(clubs.getContent(), userId));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getClubStats(@AuthenticationPrincipal Long userId) {
        long totalClubs = clubService.countPublicClubs();
        int userClubs = clubService.getClubsByUserId(userId).size();
        long totalMembers = clubService.countTotalMembers();
        return ResponseEntity.ok(Map.of(
                "totalClubs", totalClubs,
                "userClubs", userClubs,
                "totalMembers", totalMembers
        ));
    }

    @GetMapping("/{clubId}")
    public ResponseEntity<ClubResponse> getClubById(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId) {
        Club club = clubService.getClubById(clubId);
        return ResponseEntity.ok(mapAll(List.of(club), userId).getFirst());
    }

    @GetMapping("/my")
    public ResponseEntity<List<ClubResponse>> getMyClubs(@AuthenticationPrincipal Long userId) {
        List<Club> clubs = clubService.getClubsByUserId(userId);
        return ResponseEntity.ok(mapAll(clubs, userId));
    }

    @PostMapping
    public ResponseEntity<ClubResponse> createClub(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ClubRequest request) {
        Club club = mapToEntity(request);
        Club savedClub = clubService.createClub(club, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapAll(List.of(savedClub), userId).getFirst());
    }

    @PutMapping("/{clubId}")
    public ResponseEntity<ClubResponse> updateClub(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @Valid @RequestBody ClubRequest request) {
        Club club = mapToEntity(request);
        Club updatedClub = clubService.updateClub(clubId, club, userId);
        return ResponseEntity.ok(mapAll(List.of(updatedClub), userId).getFirst());
    }

    @DeleteMapping("/{clubId}")
    public ResponseEntity<Void> deleteClub(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId) {
        clubService.deleteClub(clubId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{clubId}/join")
    public ResponseEntity<Map<String, String>> joinClub(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId) {
        clubService.joinClub(clubId, userId);
        return ResponseEntity.ok(Map.of("message", "Successfully joined the club"));
    }

    @PostMapping("/{clubId}/leave")
    public ResponseEntity<Map<String, String>> leaveClub(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId) {
        clubService.leaveClub(clubId, userId);
        return ResponseEntity.ok(Map.of("message", "Successfully left the club"));
    }

    @GetMapping("/{clubId}/members")
    public ResponseEntity<List<BaseUserResponse>> getClubMembers(@PathVariable Long clubId) {
        List<Long> memberIds = clubService.getClubMemberIds(clubId);
        Map<Long, UserSummary> users = userBatchClient.usersByIds(memberIds);
        List<BaseUserResponse> response = memberIds.stream()
                .map(id -> users.containsKey(id) ? users.get(id).toBaseUserResponse() : UserSummary.unknown(id))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ClubResponse>> searchClubs(
            @AuthenticationPrincipal Long userId,
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Club> clubs = clubService.searchClubs(q, pageable);
        return ResponseEntity.ok(mapAll(clubs.getContent(), userId));
    }

    private List<ClubResponse> mapAll(List<Club> clubs, Long currentUserId) {
        Set<Long> ownerIds = clubs.stream()
                .map(Club::getOwnerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserSummary> owners = userBatchClient.usersByIds(ownerIds);
        return clubs.stream()
                .map(club -> mapToResponse(club, currentUserId, owners))
                .collect(Collectors.toList());
    }

    private ClubResponse mapToResponse(Club club, Long currentUserId, Map<Long, UserSummary> owners) {
        ClubResponse response = new ClubResponse();
        response.setId(club.getId());
        response.setName(club.getName());
        response.setDescription(club.getDescription());
        response.setCoverImage(club.getCoverImage());
        response.setProfileImage(club.getProfileImage());
        response.setCity(club.getCity());
        response.setCountry(club.getCountry());
        response.setSportType(club.getSportType());
        response.setMembersCount(club.getMembersCount());
        response.setActivitiesCount(0);
        response.setPrivate(club.isPrivate());
        response.setMember(clubService.isUserMember(club.getId(), currentUserId));
        if (club.getOwnerId() != null) {
            UserSummary owner = owners.get(club.getOwnerId());
            response.setOwner(owner != null ? owner.toBaseUserResponse() : UserSummary.unknown(club.getOwnerId()));
        }
        return response;
    }

    private Club mapToEntity(ClubRequest request) {
        return Club.builder()
                .name(request.getName())
                .description(request.getDescription())
                .coverImage(request.getCoverImage())
                .profileImage(request.getProfileImage())
                .city(request.getCity())
                .country(request.getCountry())
                .sportType(request.getSportType())
                .website(request.getWebsite())
                .isPrivate(request.isPrivate())
                .build();
    }
}
