package com.strava.backend.controller;

import com.strava.backend.domain.Challenge;
import com.strava.backend.dto.challenge.ChallengeRequest;
import com.strava.backend.dto.challenge.ChallengeResponse;
import com.strava.backend.service.ChallengeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.strava.backend.constants.PathConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_CHALLENGES)
public class ChallengeController {

    private static final Long DEFAULT_USER_ID = 1L;

    private final ChallengeService challengeService;

    @GetMapping
    public ResponseEntity<List<ChallengeResponse>> getActiveChallenges(
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        Page<Challenge> challenges = challengeService.getActiveChallenges(pageable);
        List<ChallengeResponse> response = challenges.stream()
                .map(c -> mapToResponse(c, currentUserId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping(UPCOMING)
    public ResponseEntity<List<ChallengeResponse>> getUpcomingChallenges(
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        Page<Challenge> challenges = challengeService.getUpcomingChallenges(pageable);
        List<ChallengeResponse> response = challenges.stream()
                .map(c -> mapToResponse(c, currentUserId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping(CHALLENGE_ID)
    public ResponseEntity<ChallengeResponse> getChallengeById(
            @PathVariable Long challengeId,
            @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        Challenge challenge = challengeService.getChallengeById(challengeId);
        return ResponseEntity.ok(mapToResponse(challenge, currentUserId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ChallengeResponse>> getMyChallenges(
            @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        List<Challenge> challenges = challengeService.getUserActiveChallenges(currentUserId);
        List<ChallengeResponse> response = challenges.stream()
                .map(c -> mapToResponse(c, currentUserId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ChallengeResponse> createChallenge(
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody ChallengeRequest request) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        Challenge challenge = mapToEntity(request);
        Challenge savedChallenge = challengeService.createChallenge(challenge, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(savedChallenge, currentUserId));
    }

    @PutMapping(CHALLENGE_ID)
    public ResponseEntity<ChallengeResponse> updateChallenge(
            @PathVariable Long challengeId,
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody ChallengeRequest request) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        Challenge challenge = mapToEntity(request);
        Challenge updatedChallenge = challengeService.updateChallenge(challengeId, challenge, currentUserId);
        return ResponseEntity.ok(mapToResponse(updatedChallenge, currentUserId));
    }

    @DeleteMapping(CHALLENGE_ID)
    public ResponseEntity<Void> deleteChallenge(
            @PathVariable Long challengeId,
            @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        challengeService.deleteChallenge(challengeId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(CHALLENGE_ID + PARTICIPATE)
    public ResponseEntity<Map<String, String>> joinChallenge(
            @PathVariable Long challengeId,
            @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
        challengeService.joinChallenge(challengeId, currentUserId);
        return ResponseEntity.ok(Map.of("message", "Successfully joined the challenge"));
    }

    @DeleteMapping(CHALLENGE_ID + PARTICIPATE)
    public ResponseEntity<Map<String, String>> leaveChallenge(
            @PathVariable Long challengeId,
            @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
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
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Long currentUserId = userId != null ? userId : DEFAULT_USER_ID;
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
