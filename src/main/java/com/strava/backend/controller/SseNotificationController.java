package com.strava.backend.controller;

import com.strava.backend.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static com.strava.backend.constants.PathConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_NOTIFICATIONS)
public class SseNotificationController extends BaseController {

    private final SseEmitterService sseEmitterService;


    @GetMapping(value = STREAM, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications() {
        Long currentUserId = getAuthenticatedUserId();
        return sseEmitterService.createEmitter(currentUserId);
    }
}
