package com.strava.backend.controller;

import com.strava.backend.dto.DashboardResponse;
import com.strava.backend.security.UserPrincipal;
import com.strava.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.strava.backend.constants.PathConstants.API_V1_DASHBOARD;

@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_DASHBOARD)
public class DashboardController {

    private final DashboardService dashboardService;

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getId();
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard() {
        Long currentUserId = getAuthenticatedUserId();
        return ResponseEntity.ok(dashboardService.getDashboardData(currentUserId));
    }
}
