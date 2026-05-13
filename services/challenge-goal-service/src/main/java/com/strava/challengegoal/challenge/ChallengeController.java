package com.strava.challengegoal.challenge;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challenges;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChallengeDto create(@AuthenticationPrincipal Long userId,
                               @Valid @RequestBody CreateChallengeRequest req) {
        return challenges.create(userId, req);
    }

    @GetMapping
    public Page<ChallengeDto> active(Pageable pageable) {
        return challenges.activePublic(pageable);
    }

    @GetMapping("/{id}")
    public ChallengeDto get(@PathVariable Long id) {
        return challenges.get(id);
    }

    @PostMapping("/{id}/join")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void join(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        challenges.join(id, userId);
    }

    @DeleteMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        challenges.leave(id, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        challenges.delete(id, userId);
    }

    @GetMapping("/{id}/progress")
    public ChallengeProgressDto myProgress(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return challenges.progress(id, userId);
    }

    @GetMapping("/{id}/leaderboard")
    public List<ChallengeProgressDto> leaderboard(@PathVariable Long id) {
        return challenges.leaderboard(id);
    }
}
