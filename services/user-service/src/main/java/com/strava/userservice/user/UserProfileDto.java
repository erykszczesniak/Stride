package com.strava.userservice.user;

import com.strava.userservice.domain.User;

import java.time.LocalDateTime;

public record UserProfileDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        String avatar,
        String bio,
        String city,
        String country,
        Double weight,
        Double height,
        boolean isPrivate,
        boolean hideFromSearch,
        long followers,
        long following,
        LocalDateTime createdAt
) {
    public static UserProfileDto from(User u, long followers, long following) {
        return new UserProfileDto(
                u.getId(), u.getFirstName(), u.getLastName(), u.getEmail(),
                u.getAvatar(), u.getBio(), u.getCity(), u.getCountry(),
                u.getWeight(), u.getHeight(),
                Boolean.TRUE.equals(u.getIsPrivate()),
                Boolean.TRUE.equals(u.getHideFromSearch()),
                followers, following,
                u.getCreatedAt()
        );
    }
}
