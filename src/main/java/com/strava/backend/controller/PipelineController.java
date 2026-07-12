package com.strava.backend.controller;

import com.strava.backend.dto.pipeline.PipelineActivityDetailDto;
import com.strava.backend.dto.pipeline.PipelineActivityDto;
import com.strava.backend.dto.pipeline.PipelineDashboardDto;
import com.strava.backend.dto.pipeline.PipelineErrorDto;
import com.strava.backend.dto.pipeline.PipelineLoadDto;
import com.strava.backend.dto.pipeline.PipelineProfileDto;
import com.strava.backend.dto.pipeline.PipelineRecordsDto;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.service.pipeline.PipelineQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.strava.backend.constants.PathConstants.PIPELINE_V1;

/**
 * Read-only facade serving the triathlon-tracker client's contract. Auth is a
 * personal pipeline token (see PipelineTokenFilter); every endpoint returns
 * data of the token owner only, except activity detail, which also serves
 * activities the owner may view (feed tap-through). Errors use the contract's
 * {@code {"code","message"}} body, handled locally so the global handler's
 * /api/v1 error shape stays untouched.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(PIPELINE_V1)
public class PipelineController extends BaseController {

    private final PipelineQueryService pipelineQueryService;

    @GetMapping("/dashboard")
    public ResponseEntity<PipelineDashboardDto> dashboard() {
        return ResponseEntity.ok(pipelineQueryService.dashboard(getAuthenticatedUserId()));
    }

    @GetMapping("/activities")
    public ResponseEntity<List<PipelineActivityDto>> activities() {
        return ResponseEntity.ok(pipelineQueryService.listActivities(getAuthenticatedUserId()));
    }

    @GetMapping("/activities/{activityId}")
    public ResponseEntity<PipelineActivityDetailDto> activityDetail(@PathVariable String activityId) {
        return ResponseEntity.ok(pipelineQueryService.activityDetail(getAuthenticatedUserId(), activityId));
    }

    @GetMapping("/metrics/load")
    public ResponseEntity<PipelineLoadDto> load(@RequestParam(required = false) String range) {
        return ResponseEntity.ok(pipelineQueryService.load(getAuthenticatedUserId(), range));
    }

    @GetMapping("/records")
    public ResponseEntity<PipelineRecordsDto> records() {
        return ResponseEntity.ok(pipelineQueryService.records(getAuthenticatedUserId()));
    }

    @GetMapping("/athlete/profile")
    public ResponseEntity<PipelineProfileDto> profile() {
        return ResponseEntity.ok(pipelineQueryService.profile(getAuthenticatedUserId()));
    }

    @ExceptionHandler(ApiRequestException.class)
    public ResponseEntity<PipelineErrorDto> handleApiRequestException(ApiRequestException ex) {
        String code = ex.getStatus().is4xxClientError() ? "not_found" : "server_error";
        return ResponseEntity.status(ex.getStatus())
                .body(new PipelineErrorDto(code, ex.getMessage()));
    }
}
