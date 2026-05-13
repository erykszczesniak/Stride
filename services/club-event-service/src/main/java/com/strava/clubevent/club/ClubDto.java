package com.strava.clubevent.club;

import com.strava.clubevent.domain.Club;

import java.time.LocalDateTime;

public record ClubDto(
        Long id,
        String name,
        String description,
        String location,
        Long ownerUserId,
        boolean isPrivate,
        long memberCount,
        LocalDateTime createdAt
) {
    public static ClubDto from(Club c, long memberCount) {
        return new ClubDto(c.getId(), c.getName(), c.getDescription(), c.getLocation(),
                c.getOwnerUserId(), c.isPrivate(), memberCount, c.getCreatedAt());
    }
}
