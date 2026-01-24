package com.strava.backend.service;

import com.strava.backend.dto.DashboardResponse;

public interface DashboardService {

    DashboardResponse getDashboardData(Long userId);
}
