package com.strava.backend.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * Fired only when an update flips the {@code isPrivate} flag — not on every
 * activity update — so the projector can add or retract the fan-out rows
 * without re-running for cosmetic changes (title, description, distance).
 *
 * <p>private → public: re-fan-out to all current followers.<br>
 * public → private: drop every recipient row except the author's own.</p>
 */
@Getter
@RequiredArgsConstructor
public class ActivityVisibilityChangedEvent {
    private final Long activityId;
    private final Long actorUserId;
    private final boolean nowPrivate;
    private final LocalDateTime activityCreatedAt;
}
