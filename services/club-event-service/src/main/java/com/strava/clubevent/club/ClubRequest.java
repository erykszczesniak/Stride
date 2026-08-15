package com.strava.clubevent.club;

import com.strava.clubevent.common.ActivityType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Verbatim copy of the monolith's ClubRequest — field names and Jackson
 * behavior (e.g. the boolean binding as "private") must match exactly.
 */
@Data
public class ClubRequest {

    @NotBlank(message = "Club name is required")
    private String name;

    private String description;
    private String coverImage;
    private String profileImage;
    private String city;
    private String country;
    private ActivityType sportType;
    private String website;
    private boolean isPrivate;
}
