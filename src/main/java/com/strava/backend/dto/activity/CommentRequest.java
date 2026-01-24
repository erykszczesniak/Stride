package com.strava.backend.dto.activity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import static com.strava.backend.constants.ErrorMessage.EMPTY_COMMENT;

@Data
public class CommentRequest {

    @NotBlank(message = EMPTY_COMMENT)
    private String content;
}
