package com.strava.backend.repository.projection;

import com.strava.backend.enums.Role;

import java.time.LocalDateTime;

public interface UserProjection {
    Long getId();
    String getFirstName();
    String getLastName();
    String getEmail();
    String getAvatar();
    String getBio();
    String getCity();
    String getCountry();
    Role getRole();
    LocalDateTime getCreatedAt();
}
