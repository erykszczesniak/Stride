package com.strava.backend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Body for sending a message into an activity's AI analysis chat. */
@Data
public class ChatRequest {

    @NotBlank
    @Size(max = 2000)
    private String message;
}
