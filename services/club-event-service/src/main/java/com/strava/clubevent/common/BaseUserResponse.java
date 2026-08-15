package com.strava.clubevent.common;

import lombok.Data;

/**
 * Verbatim copy of the monolith's BaseUserResponse — serializes id, firstName,
 * lastName, avatar plus the computed fullName, exactly like main.
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
