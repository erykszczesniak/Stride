package com.strava.backend.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Fired when {@code followerUserId} starts following {@code followeeUserId}.
 * Triggers a backfill of recent public activities from the followee into the
 * follower's feed — without it, the follower would only see new activities and
 * the existing history would stay invisible until a fresh write.
 */
@Getter
@RequiredArgsConstructor
public class UserFollowedEvent {
    private final Long followerUserId;
    private final Long followeeUserId;
}
