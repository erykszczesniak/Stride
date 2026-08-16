package com.strava.activityservice.gear;

import com.strava.activityservice.domain.Gear;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Public /api/v1/gear surface, mirroring main's GearController. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/gear")
public class GearController {

    private final GearService gearService;
    private final GearMapper gearMapper;

    @PostMapping
    public ResponseEntity<GearResponse> createGear(
            @AuthenticationPrincipal Long currentUserId,
            @Valid @RequestBody GearRequest request) {
        Gear gear = gearService.createGear(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(gearMapper.toResponse(gear));
    }

    @GetMapping
    public ResponseEntity<List<GearResponse>> getMyGear(
            @AuthenticationPrincipal Long currentUserId,
            @RequestParam(defaultValue = "false") boolean includeRetired) {
        List<Gear> gear = gearService.getUserGear(currentUserId, includeRetired);
        return ResponseEntity.ok(gearMapper.toResponseList(gear));
    }

    @GetMapping("/{gearId}")
    public ResponseEntity<GearResponse> getGear(
            @AuthenticationPrincipal Long currentUserId,
            @PathVariable Long gearId) {
        Gear gear = gearService.getGearById(gearId, currentUserId);
        return ResponseEntity.ok(gearMapper.toResponse(gear));
    }

    @PutMapping("/{gearId}")
    public ResponseEntity<GearResponse> updateGear(
            @AuthenticationPrincipal Long currentUserId,
            @PathVariable Long gearId,
            @Valid @RequestBody GearRequest request) {
        Gear gear = gearService.updateGear(gearId, currentUserId, request);
        return ResponseEntity.ok(gearMapper.toResponse(gear));
    }

    @DeleteMapping("/{gearId}")
    public ResponseEntity<Void> deleteGear(
            @AuthenticationPrincipal Long currentUserId,
            @PathVariable Long gearId) {
        gearService.deleteGear(gearId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{gearId}/retire")
    public ResponseEntity<GearResponse> retireGear(
            @AuthenticationPrincipal Long currentUserId,
            @PathVariable Long gearId) {
        Gear gear = gearService.retireGear(gearId, currentUserId);
        return ResponseEntity.ok(gearMapper.toResponse(gear));
    }
}
