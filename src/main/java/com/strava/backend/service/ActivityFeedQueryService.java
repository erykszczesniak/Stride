package com.strava.backend.service;

import com.strava.backend.domain.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Query side of the activity-feed CQRS split. Reads from the
 * {@code user_feed_entries} read model — never from the {@code activities}
 * write model directly. Pairs with {@code UserFeedProjector} which keeps the
 * read model in sync via domain events.
 */
public interface ActivityFeedQueryService {

    /**
     * @return the user's feed (own activities + activities of users they follow),
     * ordered newest-first. Backed by a single covering index on
     * {@code (recipient_user_id, activity_created_at DESC)}.
     */
    Page<Activity> getFeedForUser(Long userId, Pageable pageable);
}
