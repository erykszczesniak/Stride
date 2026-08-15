package com.strava.userservice.search;

import com.strava.userservice.domain.User;

/**
 * In-process event fired after a {@link User} has been created or updated so the
 * Elasticsearch projection can re-sync. Distinct from the Kafka
 * {@code com.strava.shared.events.UserUpdatedEvent}, which targets other services.
 */
public record UserUpsertedEvent(User user) {}
