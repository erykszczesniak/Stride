package com.strava.activityservice.importactivity;

import com.strava.activityservice.domain.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Public /api/v1/import surface, mirroring main's ImportController. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/import")
public class ImportController {

    private final ImportMapper importMapper;
    private final ImportService importService;

    @PostMapping("/activities")
    public ResponseEntity<ImportResponse> importActivity(
            @AuthenticationPrincipal Long currentUserId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "activityType", required = false) ActivityType activityType,
            @RequestParam(value = "isPrivate", defaultValue = "false") boolean isPrivate) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(importMapper.importActivity(currentUserId, file, title, activityType, isPrivate));
    }

    @GetMapping("/samples")
    public ResponseEntity<List<SampleFileInfo>> listSampleFiles() {
        return ResponseEntity.ok(importService.listSampleFiles());
    }

    @GetMapping("/samples/{filename}")
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
