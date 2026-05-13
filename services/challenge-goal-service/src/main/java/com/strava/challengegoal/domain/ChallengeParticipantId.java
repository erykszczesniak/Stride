package com.strava.challengegoal.domain;

import java.io.Serializable;
import java.util.Objects;

public class ChallengeParticipantId implements Serializable {

    private Long challengeId;
    private Long userId;

    public ChallengeParticipantId() {}
    public ChallengeParticipantId(Long challengeId, Long userId) {
        this.challengeId = challengeId;
        this.userId = userId;
    }

    public Long getChallengeId() { return challengeId; }
    public Long getUserId() { return userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChallengeParticipantId p)) return false;
        return Objects.equals(challengeId, p.challengeId) && Objects.equals(userId, p.userId);
    }

    @Override
    public int hashCode() { return Objects.hash(challengeId, userId); }
}
