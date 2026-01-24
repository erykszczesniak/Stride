package com.strava.backend.controller;

import com.strava.backend.dto.SearchResponse;
import com.strava.backend.dto.activity.ActivityResponse;
import com.strava.backend.dto.challenge.ChallengeResponse;
import com.strava.backend.dto.club.ClubResponse;
import com.strava.backend.dto.event.EventResponse;
import com.strava.backend.dto.user.BaseUserResponse;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.strava.backend.constants.PathConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_SEARCH)
public class SearchController {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final ClubRepository clubRepository;
    private final ChallengeRepository challengeRepository;
    private final EventRepository eventRepository;
    private final CommonMapper commonMapper;

    @GetMapping
    public ResponseEntity<SearchResponse> globalSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {

        PageRequest pageRequest = PageRequest.of(0, limit);

        var users = userRepository.searchUsers(query, pageRequest);
        var activities = activityRepository.searchActivities(query, pageRequest);
        var clubs = clubRepository.searchClubs(query, pageRequest);
        var challenges = challengeRepository.searchChallenges(query, pageRequest);
        var events = eventRepository.searchEvents(query, pageRequest);

        SearchResponse response = SearchResponse.builder()
                .users(commonMapper.convertToResponseList(users.getContent(), BaseUserResponse.class))
                .activities(commonMapper.convertToResponseList(activities.getContent(), ActivityResponse.class))
                .clubs(commonMapper.convertToResponseList(clubs.getContent(), ClubResponse.class))
                .challenges(commonMapper.convertToResponseList(challenges.getContent(), ChallengeResponse.class))
                .events(commonMapper.convertToResponseList(events.getContent(), EventResponse.class))
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping(USERS)
    public ResponseEntity<List<BaseUserResponse>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {

        PageRequest pageRequest = PageRequest.of(0, limit);
        var users = userRepository.searchUsers(query, pageRequest);
        return ResponseEntity.ok(commonMapper.convertToResponseList(users.getContent(), BaseUserResponse.class));
    }

    @GetMapping(ACTIVITIES)
    public ResponseEntity<List<ActivityResponse>> searchActivities(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {

        PageRequest pageRequest = PageRequest.of(0, limit);
        var activities = activityRepository.searchActivities(query, pageRequest);
        return ResponseEntity.ok(commonMapper.convertToResponseList(activities.getContent(), ActivityResponse.class));
    }

    @GetMapping(CLUBS)
    public ResponseEntity<List<ClubResponse>> searchClubs(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {

        PageRequest pageRequest = PageRequest.of(0, limit);
        var clubs = clubRepository.searchClubs(query, pageRequest);
        return ResponseEntity.ok(commonMapper.convertToResponseList(clubs.getContent(), ClubResponse.class));
    }

    @GetMapping(CHALLENGES)
    public ResponseEntity<List<ChallengeResponse>> searchChallenges(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {

        PageRequest pageRequest = PageRequest.of(0, limit);
        var challenges = challengeRepository.searchChallenges(query, pageRequest);
        return ResponseEntity.ok(commonMapper.convertToResponseList(challenges.getContent(), ChallengeResponse.class));
    }

    @GetMapping(EVENTS)
    public ResponseEntity<List<EventResponse>> searchEvents(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {

        PageRequest pageRequest = PageRequest.of(0, limit);
        var events = eventRepository.searchEvents(query, pageRequest);
        return ResponseEntity.ok(commonMapper.convertToResponseList(events.getContent(), EventResponse.class));
    }
}
