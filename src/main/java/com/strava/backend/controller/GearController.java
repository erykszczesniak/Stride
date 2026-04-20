package com.strava.backend.controller;

import com.strava.backend.domain.Gear;
import com.strava.backend.dto.gear.GearRequest;
import com.strava.backend.dto.gear.GearResponse;
import com.strava.backend.mapper.GearMapper;
import com.strava.backend.service.GearService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.strava.backend.constants.PathConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_GEAR)
public class GearController extends BaseController {

    private final GearService gearService;
    private final GearMapper gearMapper;

    @PostMapping
    public ResponseEntity<GearResponse> createGear(@Valid @RequestBody GearRequest request) {
        Long userId = getAuthenticatedUserId();
        Gear gear = gearService.createGear(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(gearMapper.toResponse(gear));
    }

    @GetMapping
    public ResponseEntity<List<GearResponse>> getMyGear(
            @RequestParam(defaultValue = "false") boolean includeRetired) {
        Long userId = getAuthenticatedUserId();
        List<Gear> gear = gearService.getUserGear(userId, includeRetired);
        return ResponseEntity.ok(gearMapper.toResponseList(gear));
    }

    @GetMapping(GEAR_ID)
    public ResponseEntity<GearResponse> getGear(@PathVariable Long gearId) {
        Long userId = getAuthenticatedUserId();
        Gear gear = gearService.getGearById(gearId, userId);
        return ResponseEntity.ok(gearMapper.toResponse(gear));
    }

    @PutMapping(GEAR_ID)
    public ResponseEntity<GearResponse> updateGear(
            @PathVariable Long gearId,
            @Valid @RequestBody GearRequest request) {
        Long userId = getAuthenticatedUserId();
        Gear gear = gearService.updateGear(gearId, userId, request);
        return ResponseEntity.ok(gearMapper.toResponse(gear));
    }

    @DeleteMapping(GEAR_ID)
    public ResponseEntity<Void> deleteGear(@PathVariable Long gearId) {
        Long userId = getAuthenticatedUserId();
        gearService.deleteGear(gearId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(GEAR_ID + RETIRE)
    public ResponseEntity<GearResponse> retireGear(@PathVariable Long gearId) {
        Long userId = getAuthenticatedUserId();
        Gear gear = gearService.retireGear(gearId, userId);
        return ResponseEntity.ok(gearMapper.toResponse(gear));
    }
}
