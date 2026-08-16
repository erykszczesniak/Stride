package com.strava.webbff.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/v1/dashboard — same contract as the monolith's DashboardController.
 * Deliberately no {@code /stats} sub-mapping: main never mapped
 * GET /api/v1/dashboard/stats either (the frontend's getStatsSummary 404s
 * there too), and parity includes the holes.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(dashboardService.getDashboardData());
    }
}
