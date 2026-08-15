package com.strava.activityservice.activity;

import com.strava.activityservice.client.UserVisibilityClient;
import com.strava.activityservice.domain.Activity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * The one place that decides whether a viewer may see an activity, ported
 * from main: owners see everything of their own; others need a public
 * activity and either a public owner profile or a follow relationship.
 *
 * The owner-profile privacy flag and the follow fact live in user-service —
 * fetched through UserVisibilityClient (fail-open to "profile not private";
 * activity-level isPrivate is local and always enforced).
 */
@Service
@RequiredArgsConstructor
public class ActivityVisibility {

    private final UserVisibilityClient userVisibilityClient;

    public boolean isViewableBy(Long viewerId, Activity activity) {
        Long ownerId = activity.getUserId();
        if (ownerId == null) {
            return false;
        }
        if (viewerId != null && viewerId.equals(ownerId)) {
            return true;
        }
        if (activity.isPrivate()) {
            return false;
        }
        UserVisibilityClient.VisibilityInfo owner = userVisibilityClient.visibilityFor(ownerId, viewerId);
        boolean privateProfile = owner.isPrivate();
        return !privateProfile || (viewerId != null && owner.viewerFollows());
    }
}
