package com.strava.backend.event;

import lombok.Getter;

/**
 * Fired after a user is deleted so that downstream projections (Elasticsearch
 * index, etc.) can remove the corresponding document.
 */
@Getter
public class UserDeletedEvent {

    private final Long userId;

    public UserDeletedEvent(Long userId) {
        this.userId = userId;
    }
}
