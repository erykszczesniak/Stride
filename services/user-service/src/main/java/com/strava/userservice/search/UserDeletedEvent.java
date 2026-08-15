package com.strava.userservice.search;

/**
 * In-process event fired after a user is deleted so the Elasticsearch projection can
 * drop the corresponding document. Distinct from the Kafka
 * {@code com.strava.shared.events.UserDeletedEvent}, which targets other services.
 */
public record UserDeletedEvent(Long userId) {}
