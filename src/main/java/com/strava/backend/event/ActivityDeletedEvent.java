package com.strava.backend.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Fired after an {@code Activity} is deleted so the feed read model can drop
 * every fan-out row pointing at it. Defensive — the FK on
 * {@code user_feed_entries.activity_id} also cascades — but emitting the event
 * keeps the projection self-healing if the constraint is ever loosened.
 */
@Getter
@RequiredArgsConstructor
public class ActivityDeletedEvent {
    private final Long activityId;
}
