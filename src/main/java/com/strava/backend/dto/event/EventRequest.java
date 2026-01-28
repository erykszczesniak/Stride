package com.strava.backend.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.strava.backend.enums.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventRequest {

    @NotBlank(message = "Event name is required")
    private String name;

    private String description;
    private String coverImage;
    private ActivityType activityType;

    @NotNull(message = "Event date is required")
    private LocalDateTime eventDate;

    private LocalDateTime registrationDeadline;

    @NotBlank(message = "Location is required")
    private String location;

    private String city;
    private String country;
    private Double distance;
    private Double latitude;
    private Double longitude;
    private Integer maxParticipants;
    private Double entryFee;
    private String currency;
    private String externalUrl;

    @JsonProperty("isPublic")
    private boolean isPublic;

    private Long clubId;
}
