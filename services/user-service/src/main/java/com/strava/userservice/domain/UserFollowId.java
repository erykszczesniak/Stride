package com.strava.userservice.domain;

import java.io.Serializable;
import java.util.Objects;

public class UserFollowId implements Serializable {

    private Long userId;
    private Long followerId;

    public UserFollowId() {}

    public UserFollowId(Long userId, Long followerId) {
        this.userId = userId;
        this.followerId = followerId;
    }

    public Long getUserId() { return userId; }
    public Long getFollowerId() { return followerId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserFollowId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(followerId, that.followerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, followerId);
    }
}
