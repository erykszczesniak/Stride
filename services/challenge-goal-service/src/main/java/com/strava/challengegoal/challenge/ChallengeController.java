package com.strava.challengegoal.challenge;

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
import java.util.stream.Collectors;

/**
 * Public contract identical to the monolith's ChallengeController
 * (/api/v1/challenges — plain JSON array list envelopes, participate verbs,
 * leaderboard maps).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/challenges")
public class ChallengeController {

    private final ChallengeService challengeService;

    @GetMapping
    public ResponseEntity<List<ChallengeResponse>> getActiveChallenges(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Challenge> challenges = challengeService.getActiveChallenges(pageable);
        List<ChallengeResponse> response = challenges.stream()
                .map(c -> mapToResponse(c, userId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<ChallengeResponse>> getUpcomingChallenges(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Challenge> challenges = challengeService.getUpcomingChallenges(pageable);
        List<ChallengeResponse> response = challenges.stream()
                .map(c -> mapToResponse(c, userId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{challengeId}")
    public ResponseEntity<ChallengeResponse> getChallengeById(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long challengeId) {
        Challenge challenge = challengeService.getChallengeById(challengeId);
        return ResponseEntity.ok(mapToResponse(challenge, userId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ChallengeResponse>> getMyChallenges(@AuthenticationPrincipal Long userId) {
        List<Challenge> challenges = challengeService.getUserActiveChallenges(userId);
        List<ChallengeResponse> response = challenges.stream()
                .map(c -> mapToResponse(c, userId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ChallengeResponse> createChallenge(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChallengeRequest request) {
        Challenge challenge = mapToEntity(request);
        Challenge savedChallenge = challengeService.createChallenge(challenge, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(savedChallenge, userId));
    }

    @PutMapping("/{challengeId}")
    public ResponseEntity<ChallengeResponse> updateChallenge(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long challengeId,
            @Valid @RequestBody ChallengeRequest request) {
        Challenge challenge = mapToEntity(request);
        Challenge updatedChallenge = challengeService.updateChallenge(challengeId, challenge, userId);
        return ResponseEntity.ok(mapToResponse(updatedChallenge, userId));
    }

    @DeleteMapping("/{challengeId}")
    public ResponseEntity<Void> deleteChallenge(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long challengeId) {
        challengeService.deleteChallenge(challengeId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{challengeId}/participate")
    public ResponseEntity<Map<String, String>> joinChallenge(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long challengeId) {
        challengeService.joinChallenge(challengeId, userId);
        return ResponseEntity.ok(Map.of("message", "Successfully joined the challenge"));
    }

    @DeleteMapping("/{challengeId}/participate")
    public ResponseEntity<Map<String, String>> leaveChallenge(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long challengeId) {
        challengeService.leaveChallenge(challengeId, userId);
        return ResponseEntity.ok(Map.of("message", "Successfully left the challenge"));
    }

    @GetMapping("/{challengeId}/leaderboard")
    public ResponseEntity<List<Map<String, Object>>> getChallengeLeaderboard(
            @PathVariable Long challengeId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(challengeService.getLeaderboard(challengeId, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ChallengeResponse>> searchChallenges(
            @AuthenticationPrincipal Long userId,
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Challenge> challenges = challengeService.searchChallenges(q, pageable);
        List<ChallengeResponse> response = challenges.stream()
                .map(c -> mapToResponse(c, userId))
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
        response.setCreatorId(challenge.getCreatorId());
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
