package com.strava.clubevent.domain;

import java.io.Serializable;
import java.util.Objects;

public class ClubMemberId implements Serializable {

    private Long clubId;
    private Long userId;

    public ClubMemberId() {}
    public ClubMemberId(Long clubId, Long userId) {
        this.clubId = clubId;
        this.userId = userId;
    }

    public Long getClubId() { return clubId; }
    public Long getUserId() { return userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClubMemberId other)) return false;
        return Objects.equals(clubId, other.clubId) && Objects.equals(userId, other.userId);
    }

    @Override
    public int hashCode() { return Objects.hash(clubId, userId); }
}
