package com.strava.backend.service;

import com.strava.backend.dto.training.TrainingPlanTemplateResponse;

import java.util.List;

public interface TrainingPlanTemplateService {

    List<TrainingPlanTemplateResponse> getAllTemplates();

    TrainingPlanTemplateResponse getTemplateById(String id);
}
