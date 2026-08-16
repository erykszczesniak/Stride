package com.strava.webbff.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/v1/admin/stats — same contract as the monolith's AdminController
 * stats endpoint. Guarded twice, like the other services: the URL rule in
 * {@code SecurityConfig} plus method security here.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @GetMapping
    public ResponseEntity<AdminStatsResponse> getAdminStats() {
        return ResponseEntity.ok(adminStatsService.getAdminStats());
    }
}
