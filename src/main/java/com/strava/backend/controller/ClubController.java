package com.strava.backend.controller;

import com.strava.backend.domain.Club;
import com.strava.backend.dto.club.ClubRequest;
import com.strava.backend.dto.club.ClubResponse;
import com.strava.backend.dto.user.BaseUserResponse;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.security.UserPrincipal;
import com.strava.backend.service.ClubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.strava.backend.constants.PathConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_CLUBS)
public class ClubController {

    private final ClubService clubService;
    private final CommonMapper commonMapper;

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getId();
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<List<ClubResponse>> getAllClubs(
            @RequestParam(required = false) ActivityType sportType,
            @PageableDefault(size = 20) Pageable pageable) {
        Long currentUserId = getAuthenticatedUserId();
        Page<Club> clubs;
        if (sportType != null) {
            clubs = clubService.getClubsBySportType(sportType, pageable);
        } else {
            clubs = clubService.getAllPublicClubs(pageable);
        }
        List<ClubResponse> response = clubs.stream()
                .map(club -> mapToResponse(club, currentUserId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getClubStats() {
        Long currentUserId = getAuthenticatedUserId();
        long totalClubs = clubService.countPublicClubs();
        int userClubs = clubService.getClubsByUserId(currentUserId).size();
        long totalMembers = clubService.countTotalMembers();
        return ResponseEntity.ok(Map.of(
                "totalClubs", totalClubs,
                "userClubs", userClubs,
                "totalMembers", totalMembers
        ));
    }

    @GetMapping(CLUB_ID)
    public ResponseEntity<ClubResponse> getClubById(@PathVariable Long clubId) {
        Long currentUserId = getAuthenticatedUserId();
        Club club = clubService.getClubById(clubId);
        return ResponseEntity.ok(mapToResponse(club, currentUserId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ClubResponse>> getMyClubs() {
        Long currentUserId = getAuthenticatedUserId();
        List<Club> clubs = clubService.getClubsByUserId(currentUserId);
        List<ClubResponse> response = clubs.stream()
                .map(club -> mapToResponse(club, currentUserId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ClubResponse> createClub(@Valid @RequestBody ClubRequest request) {
        Long currentUserId = getAuthenticatedUserId();
        Club club = mapToEntity(request);
        Club savedClub = clubService.createClub(club, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(savedClub, currentUserId));
    }

    @PutMapping(CLUB_ID)
    public ResponseEntity<ClubResponse> updateClub(
            @PathVariable Long clubId,
            @Valid @RequestBody ClubRequest request) {
        Long currentUserId = getAuthenticatedUserId();
        Club club = mapToEntity(request);
        Club updatedClub = clubService.updateClub(clubId, club, currentUserId);
        return ResponseEntity.ok(mapToResponse(updatedClub, currentUserId));
    }

    @DeleteMapping(CLUB_ID)
    public ResponseEntity<Void> deleteClub(@PathVariable Long clubId) {
        Long currentUserId = getAuthenticatedUserId();
        clubService.deleteClub(clubId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(CLUB_ID + JOIN)
    public ResponseEntity<Map<String, String>> joinClub(@PathVariable Long clubId) {
        Long currentUserId = getAuthenticatedUserId();
        clubService.joinClub(clubId, currentUserId);
        return ResponseEntity.ok(Map.of("message", "Successfully joined the club"));
    }

    @PostMapping(CLUB_ID + LEAVE)
    public ResponseEntity<Map<String, String>> leaveClub(@PathVariable Long clubId) {
        Long currentUserId = getAuthenticatedUserId();
        clubService.leaveClub(clubId, currentUserId);
        return ResponseEntity.ok(Map.of("message", "Successfully left the club"));
    }

    @GetMapping(CLUB_ID + MEMBERS)
    public ResponseEntity<List<BaseUserResponse>> getClubMembers(@PathVariable Long clubId) {
        List<?> members = clubService.getClubMembers(clubId);
        List<BaseUserResponse> response = members.stream()
                .map(m -> commonMapper.convertToResponse(m, BaseUserResponse.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping(SEARCH)
    public ResponseEntity<List<ClubResponse>> searchClubs(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        Long currentUserId = getAuthenticatedUserId();
        Page<Club> clubs = clubService.searchClubs(q, pageable);
        List<ClubResponse> response = clubs.stream()
                .map(club -> mapToResponse(club, currentUserId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    private ClubResponse mapToResponse(Club club, Long currentUserId) {
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
        response.setActivitiesCount(club.getMembersCount() * 12 + (int)(club.getId() * 100));
        response.setPrivate(club.isPrivate());
        response.setMember(clubService.isUserMember(club.getId(), currentUserId));
        if (club.getOwner() != null) {
            response.setOwner(commonMapper.convertToResponse(club.getOwner(), BaseUserResponse.class));
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
