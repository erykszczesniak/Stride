package com.strava.backend.event;

import com.strava.backend.domain.User;
import lombok.Getter;

/**
 * Fired after a {@link User} has been created or updated so that downstream
 * projections (Elasticsearch index, etc.) can re-sync.
 */
@Getter
public class UserUpsertedEvent {

    private final User user;

    public UserUpsertedEvent(User user) {
        this.user = user;
    }
}
