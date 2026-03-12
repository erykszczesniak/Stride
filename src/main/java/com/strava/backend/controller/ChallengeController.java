package com.strava.backend.controller;

import com.strava.backend.domain.Challenge;
import com.strava.backend.dto.challenge.ChallengeRequest;
import com.strava.backend.dto.challenge.ChallengeResponse;
import com.strava.backend.security.UserPrincipal;
import com.strava.backend.service.ChallengeService;
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
@RequestMapping(API_V1_CHALLENGES)
public class ChallengeController {

    private final ChallengeService challengeService;

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getId();
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<List<ChallengeResponse>> getActiveChallenges(
            @PageableDefault(size = 20) Pageable pageable) {
        Long currentUserId = getAuthenticatedUserId();
        Page<Challenge> challenges = challengeService.getActiveChallenges(pageable);
        List<ChallengeResponse> response = challenges.stream()
                .map(c -> mapToResponse(c, currentUserId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping(UPCOMING)
    public ResponseEntity<List<ChallengeResponse>> getUpcomingChallenges(
            @PageableDefault(size = 20) Pageable pageable) {
        Long currentUserId = getAuthenticatedUserId();
        Page<Challenge> challenges = challengeService.getUpcomingChallenges(pageable);
        List<ChallengeResponse> response = challenges.stream()
                .map(c -> mapToResponse(c, currentUserId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping(CHALLENGE_ID)
    public ResponseEntity<ChallengeResponse> getChallengeById(@PathVariable Long challengeId) {
        Long currentUserId = getAuthenticatedUserId();
        Challenge challenge = challengeService.getChallengeById(challengeId);
        return ResponseEntity.ok(mapToResponse(challenge, currentUserId));
    }

    @GetMapping(MY)
    public ResponseEntity<List<ChallengeResponse>> getMyChallenges() {
        Long currentUserId = getAuthenticatedUserId();
        List<Challenge> challenges = challengeService.getUserActiveChallenges(currentUserId);
        List<ChallengeResponse> response = challenges.stream()
                .map(c -> mapToResponse(c, currentUserId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ChallengeResponse> createChallenge(
            @Valid @RequestBody ChallengeRequest request) {
        Long currentUserId = getAuthenticatedUserId();
        Challenge challenge = mapToEntity(request);
        Challenge savedChallenge = challengeService.createChallenge(challenge, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(savedChallenge, currentUserId));
    }

    @PutMapping(CHALLENGE_ID)
    public ResponseEntity<ChallengeResponse> updateChallenge(
            @PathVariable Long challengeId,
            @Valid @RequestBody ChallengeRequest request) {
        Long currentUserId = getAuthenticatedUserId();
        Challenge challenge = mapToEntity(request);
        Challenge updatedChallenge = challengeService.updateChallenge(challengeId, challenge, currentUserId);
        return ResponseEntity.ok(mapToResponse(updatedChallenge, currentUserId));
    }

    @DeleteMapping(CHALLENGE_ID)
    public ResponseEntity<Void> deleteChallenge(@PathVariable Long challengeId) {
        Long currentUserId = getAuthenticatedUserId();
        challengeService.deleteChallenge(challengeId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(CHALLENGE_ID + PARTICIPATE)
    public ResponseEntity<Map<String, String>> joinChallenge(@PathVariable Long challengeId) {
        Long currentUserId = getAuthenticatedUserId();
        challengeService.joinChallenge(challengeId, currentUserId);
        return ResponseEntity.ok(Map.of("message", "Successfully joined the challenge"));
    }

    @DeleteMapping(CHALLENGE_ID + PARTICIPATE)
    public ResponseEntity<Map<String, String>> leaveChallenge(@PathVariable Long challengeId) {
        Long currentUserId = getAuthenticatedUserId();
        challengeService.leaveChallenge(challengeId, currentUserId);
        return ResponseEntity.ok(Map.of("message", "Successfully left the challenge"));
    }

    @GetMapping(CHALLENGE_ID + LEADERBOARD)
    public ResponseEntity<List<Map<String, Object>>> getChallengeLeaderboard(
            @PathVariable Long challengeId) {
        Challenge challenge = challengeService.getChallengeById(challengeId);

        List<Map<String, Object>> leaderboard = challenge.getParticipants().stream()
                .map(user -> {
                    Double progress = challengeService.getUserProgressInChallenge(challengeId, user.getId());
                    return Map.<String, Object>of(
                            "userId", user.getId(),
                            "userName", user.getFirstName() + " " + user.getLastName(),
                            "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                            "progress", progress,
                            "percentage", Math.min((progress / challenge.getTargetValue()) * 100, 100)
                    );
                })
                .sorted((a, b) -> Double.compare((Double) b.get("progress"), (Double) a.get("progress")))
                .collect(Collectors.toList());

        return ResponseEntity.ok(leaderboard);
    }

    @GetMapping(SEARCH)
    public ResponseEntity<List<ChallengeResponse>> searchChallenges(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        Long currentUserId = getAuthenticatedUserId();
        Page<Challenge> challenges = challengeService.searchChallenges(q, pageable);
        List<ChallengeResponse> response = challenges.stream()
                .map(c -> mapToResponse(c, currentUserId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    private ChallengeResponse mapToResponse(Challenge challenge, Long currentUserId) {
        ChallengeResponse response = new ChallengeResponse();
        response.setId(challenge.getId());
        response.setName(challenge.getName());
        response.setDescription(challenge.getDescription());
        response.setCoverImage(challenge.getCoverImage());
        response.setChallengeType(challenge.getChallengeType());
        response.setActivityType(challenge.getActivityType());
        response.setTargetValue(challenge.getTargetValue());
        response.setStartDate(challenge.getStartDate());
        response.setEndDate(challenge.getEndDate());
        response.setParticipantsCount(challenge.getParticipantsCount());
        response.setActive(challenge.isActive());
        response.setPublic(challenge.isPublic());
        response.setCreatorId(challenge.getCreator().getId());
        response.setParticipating(challengeService.isUserParticipating(challenge.getId(), currentUserId));

        if (response.isParticipating()) {
            response.setUserProgress(challengeService.getUserProgressInChallenge(challenge.getId(), currentUserId));
        }

        return response;
    }

    private Challenge mapToEntity(ChallengeRequest request) {
        return Challenge.builder()
                .name(request.getName())
                .description(request.getDescription())
                .coverImage(request.getCoverImage())
                .challengeType(request.getChallengeType())
                .activityType(request.getActivityType())
                .targetValue(request.getTargetValue())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isPublic(request.isPublic())
                .build();
    }
}
