package com.strava.backend.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.strava.backend.enums.ActivityType;
import lombok.Data;

import java.time.LocalDateTime;

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
