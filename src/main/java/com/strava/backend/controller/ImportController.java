package com.strava.backend.controller;

import com.strava.backend.dto.importactivity.ImportResponse;
import com.strava.backend.dto.importactivity.SampleFileInfo;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.mapper.ImportMapper;
import com.strava.backend.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.strava.backend.constants.PathConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_IMPORT)
public class ImportController extends BaseController {

    private final ImportMapper importMapper;
    private final ImportService importService;

    @PostMapping(ACTIVITIES)
    public ResponseEntity<ImportResponse> importActivity(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "activityType", required = false) ActivityType activityType,
            @RequestParam(value = "isPrivate", defaultValue = "false") boolean isPrivate) {
        Long currentUserId = getAuthenticatedUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(importMapper.importActivity(currentUserId, file, title, activityType, isPrivate));
    }

    @GetMapping(SAMPLES)
    public ResponseEntity<List<SampleFileInfo>> listSampleFiles() {
        return ResponseEntity.ok(importService.listSampleFiles());
    }

    @GetMapping(SAMPLES + "/{filename}")
    public ResponseEntity<Resource> downloadSampleFile(@PathVariable String filename) {
        Resource resource = importService.getSampleFile(filename);
        String contentType = filename.endsWith(".gpx") ? "application/gpx+xml"
                : filename.endsWith(".tcx") ? "application/xml"
                : "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }
}
