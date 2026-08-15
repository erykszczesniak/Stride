package com.strava.clubevent.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.strava.clubevent.common.ActivityType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Verbatim copy of the monolith's EventResponse — note that unlike clubs/
 * challenges, the booleans here keep their "isRegistered"/"isPublic" JSON keys
 * via @JsonProperty, exactly like main.
 */
@Data
public class EventResponse {
    private Long id;
    private String name;
    private String description;
    private String coverImage;
    private ActivityType activityType;
    private LocalDateTime eventDate;
    private String location;
    private String city;
    private Double distance;
    private int registeredCount;
    private Integer maxParticipants;

    @JsonProperty("isRegistered")
    private boolean isRegistered;

    @JsonProperty("isPublic")
    private boolean isPublic;
}
