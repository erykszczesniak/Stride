package com.strava.backend.dto.admin;

import com.strava.backend.enums.AuthProvider;
import com.strava.backend.enums.Role;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String avatar;
    private String city;
    private String country;
    private AuthProvider authProvider;
    private Role role;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int activitiesCount;
    private int followersCount;
    private int followingCount;
}
