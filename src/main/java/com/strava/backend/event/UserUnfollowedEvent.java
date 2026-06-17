package com.strava.backend.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Fired when {@code followerUserId} stops following {@code followeeUserId}.
 * The projector purges every feed row where the recipient is the former
 * follower and the actor is the no-longer-followed user.
 */
@Getter
@RequiredArgsConstructor
public class UserUnfollowedEvent {
    private final Long followerUserId;
    private final Long followeeUserId;
}
