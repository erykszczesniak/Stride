package com.strava.backend.service;

import com.strava.backend.domain.Activity;
import com.strava.backend.dto.importactivity.SampleFileInfo;
import com.strava.backend.enums.ActivityType;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImportService {

    Activity importActivity(Long userId, MultipartFile file, String titleOverride, ActivityType activityTypeOverride, boolean isPrivate);

    List<SampleFileInfo> listSampleFiles();

    Resource getSampleFile(String filename);
}
