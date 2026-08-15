package com.strava.userservice.search;

import com.strava.userservice.domain.UserRepository;
import com.strava.userservice.user.BaseUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Global search contract ({@code /api/v1/search}), mirrored from the monolith's
 * {@code SearchController} (SQL-backed, not Elasticsearch). Only the users slice is
 * served locally — activities/clubs/challenges/events belong to other services and
 * return empty lists here; global search composition is deferred (the web app never
 * calls those per-entity endpoints).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
public class SearchController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<SearchResponse> globalSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(SearchResponse.ofUsers(searchUserCards(query, limit)));
    }

    @GetMapping("/users")
    public ResponseEntity<List<BaseUserResponse>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(searchUserCards(query, limit));
    }

    @GetMapping("/activities")
    public ResponseEntity<List<Object>> searchActivities(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/clubs")
    public ResponseEntity<List<Object>> searchClubs(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/challenges")
    public ResponseEntity<List<Object>> searchChallenges(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/events")
    public ResponseEntity<List<Object>> searchEvents(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(List.of());
    }

    private List<BaseUserResponse> searchUserCards(String query, int limit) {
        return userRepository.searchUsersByText(query, PageRequest.of(0, limit))
                .getContent().stream()
                .map(BaseUserResponse::from)
                .toList();
    }
}
