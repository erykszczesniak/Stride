package com.strava.backend.dto;

import com.strava.backend.dto.activity.ActivityResponse;
import com.strava.backend.dto.challenge.ChallengeResponse;
import com.strava.backend.dto.club.ClubResponse;
import com.strava.backend.dto.event.EventResponse;
import com.strava.backend.dto.user.BaseUserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    private List<BaseUserResponse> users;
    private List<ActivityResponse> activities;
    private List<ClubResponse> clubs;
    private List<ChallengeResponse> challenges;
    private List<EventResponse> events;
}
