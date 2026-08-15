package com.strava.activityservice.user;

import lombok.Data;

/**
 * Ported 1:1 from main — the embedded author object on activity responses.
 * getFullName() is deliberately a derived getter (Jackson serializes it as
 * "fullName"), exactly like main.
 */
@Data
public class BaseUserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String avatar;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
