package com.strava.userservice.internal;

/** Privacy verdict for a (profile owner, viewer) pair — {@code viewerFollows} means the viewer follows the owner. */
public record UserVisibilityResponse(
        boolean isPrivate,
        boolean viewerFollows
) {}
