package com.strava.backend.dto.user;

import com.strava.backend.enums.Role;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserResponse extends BaseUserResponse {
    private String email;
    private String bio;
    private String city;
    private String country;
    private Double weight;
    private Double height;
    private Role role;
    private LocalDateTime createdAt;
    private int followersCount;
    private int followingCount;
    private int activitiesCount;
}
