package com.strava.activityservice.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CLUSTER-INTERNAL API — not routed by the gateway and therefore never
 * reachable from outside; SecurityConfig permits /internal/** because callers
 * are sibling services (social feed hydration, AI context, BFF reports) that
 * carry no user identity headers. In Kubernetes a NetworkPolicy additionally
 * restricts who can reach the pod.
 *
 * The response shapes are cross-service contracts — do not change them.
 * The same data is also served over gRPC (ActivityInternalService, see the
 * grpc package); both transports delegate to InternalActivityQueryService so
 * their semantics cannot drift.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/activities")
public class InternalActivityController {

    private final InternalActivityQueryService internalActivityQueryService;

    /**
     * Batch hydration by id. Missing ids are silently skipped; order is
     * unspecified. No social counts and no embedded user object — callers
     * compose those themselves.
     */
    @GetMapping
    public ResponseEntity<List<InternalActivityResponse>> getActivities(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(internalActivityQueryService.getActivities(ids));
    }

    /** Aggregated stats for a user over [from, to] (ISO date-times). */
    @GetMapping("/stats/range")
    public ResponseEntity<Map<String, Object>> getRangeStats(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(internalActivityQueryService.getRangeStats(userId, from, to));
    }
}
