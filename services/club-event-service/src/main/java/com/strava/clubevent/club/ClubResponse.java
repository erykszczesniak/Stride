package com.strava.clubevent.club;

import com.strava.clubevent.common.ActivityType;
import com.strava.clubevent.common.BaseUserResponse;
import lombok.Data;

/**
 * Verbatim copy of the monolith's ClubResponse. The Lombok "isX" boolean
 * fields intentionally serialize as "member"/"private" — the frontend depends
 * on those exact keys.
 */
@Data
public class ClubResponse {
    private Long id;
    private String name;
    private String description;
    private String coverImage;
    private String profileImage;
    private String city;
    private String country;
    private ActivityType sportType;
    private int membersCount;
    private int activitiesCount;
    private boolean isMember;
    private boolean isPrivate;
    private BaseUserResponse owner;
}
