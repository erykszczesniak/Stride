package com.strava.backend.dto.club;

import com.strava.backend.dto.user.BaseUserResponse;
import com.strava.backend.enums.ActivityType;
import lombok.Data;

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
