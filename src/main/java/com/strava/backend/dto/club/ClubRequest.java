package com.strava.backend.dto.club;

import com.strava.backend.enums.ActivityType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

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
