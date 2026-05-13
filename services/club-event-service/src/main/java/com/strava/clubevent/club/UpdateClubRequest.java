package com.strava.clubevent.club;

import jakarta.validation.constraints.Size;

public record UpdateClubRequest(
        @Size(max = 255) String name,
        @Size(max = 2000) String description,
        @Size(max = 255) String location,
        Boolean isPrivate
) {}
