package com.strava.pipeline.pipeline;

import com.strava.pipeline.exception.ApiRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only facade serving the triathlon-tracker client's contract, ported
 * 1:1 from main. Auth is a personal pipeline token or a Stride JWT (see
 * PipelineTokenFilter); every endpoint returns data of the token owner only,
 * except activity detail, which also serves activities the owner may view
 * (feed tap-through). Errors use the contract's {@code {"code","message"}}
 * body, handled locally so the service-wide ProblemDetail advice never
 * touches the /pipeline/v1 error shape.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/pipeline/v1")
public class PipelineController {

    private final PipelineQueryService pipelineQueryService;

    @GetMapping("/dashboard")
    public ResponseEntity<PipelineDashboardDto> dashboard(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(pipelineQueryService.dashboard(userId));
    }

    @GetMapping("/activities")
    public ResponseEntity<List<PipelineActivityDto>> activities(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(pipelineQueryService.listActivities(userId));
    }

    @GetMapping("/activities/{activityId}")
    public ResponseEntity<PipelineActivityDetailDto> activityDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable String activityId) {
        return ResponseEntity.ok(pipelineQueryService.activityDetail(userId, activityId));
    }

    @GetMapping("/metrics/load")
    public ResponseEntity<PipelineLoadDto> load(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String range) {
        return ResponseEntity.ok(pipelineQueryService.load(userId, range));
    }

    @GetMapping("/records")
    public ResponseEntity<PipelineRecordsDto> records(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(pipelineQueryService.records(userId));
    }

    @GetMapping("/athlete/profile")
    public ResponseEntity<PipelineProfileDto> profile(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(pipelineQueryService.profile(userId));
    }

    @ExceptionHandler(ApiRequestException.class)
    public ResponseEntity<PipelineErrorDto> handleApiRequestException(ApiRequestException ex) {
        String code = ex.getStatus().is4xxClientError() ? "not_found" : "server_error";
        return ResponseEntity.status(ex.getStatus())
                .body(new PipelineErrorDto(code, ex.getMessage()));
    }
}
