package com.strava.backend.dto.user;

import lombok.Data;

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
