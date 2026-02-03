package com.strava.backend.controller;

import com.strava.backend.dto.training.TrainingPlanTemplateResponse;
import com.strava.backend.service.TrainingPlanTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.strava.backend.constants.PathConstants.*;

@RestController
@RequestMapping(API_V1_TRAINING_PLANS + TEMPLATES)
@RequiredArgsConstructor
public class TrainingPlanTemplateController {

    private final TrainingPlanTemplateService templateService;

    @GetMapping
    public ResponseEntity<List<TrainingPlanTemplateResponse>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @GetMapping(TEMPLATE_ID)
    public ResponseEntity<TrainingPlanTemplateResponse> getTemplateById(@PathVariable String id) {
        TrainingPlanTemplateResponse template = templateService.getTemplateById(id);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(template);
    }
}
