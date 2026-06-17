package com.strava.backend.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * Fired after an {@code Activity} is persisted so that the user-feed read model
 * can fan it out to followers' inboxes. Carries only what the projector needs —
 * the full entity isn't shipped because the listener runs after commit and
 * lazy-loaded associations would not be reachable here.
 */
@Getter
@RequiredArgsConstructor
public class ActivityCreatedEvent {
    private final Long activityId;
    private final Long actorUserId;
    private final boolean isPrivate;
    private final LocalDateTime activityCreatedAt;
}
