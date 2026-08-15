package com.strava.challengegoal.trainingplan;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public contract identical to the monolith's TrainingPlanTemplateController
 * (/api/v1/training-plans/templates).
 */
@RestController
@RequestMapping("/api/v1/training-plans/templates")
@RequiredArgsConstructor
public class TrainingPlanTemplateController {

    private final TrainingPlanTemplateService templateService;

    @GetMapping
    public ResponseEntity<List<TrainingPlanTemplateResponse>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrainingPlanTemplateResponse> getTemplateById(@PathVariable String id) {
        TrainingPlanTemplateResponse template = templateService.getTemplateById(id);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(template);
    }
}
