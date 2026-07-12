package com.strava.backend.service;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.User;
import com.strava.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * The one place that decides whether a viewer may see an activity. Mirrors
 * the feed/profile rules: owners see everything of their own; others need a
 * public activity and either a public owner profile or a follow
 * relationship. Used by the /api/v1 detail endpoint and the pipeline facade.
 */
@Service
@RequiredArgsConstructor
public class ActivityVisibility {

    private final UserRepository userRepository;

    public boolean isViewableBy(Long viewerId, Activity activity) {
        User owner = activity.getUser();
        if (owner == null) {
            return false;
        }
        if (viewerId != null && viewerId.equals(owner.getId())) {
            return true;
        }
        if (activity.isPrivate()) {
            return false;
        }
        boolean privateProfile = Boolean.TRUE.equals(owner.getIsPrivate());
        return !privateProfile
                || (viewerId != null && userRepository.isFollowing(owner.getId(), viewerId));
    }
}
