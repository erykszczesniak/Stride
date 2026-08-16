package com.strava.socialservice.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import static com.strava.socialservice.constants.ErrorMessage.EMPTY_COMMENT;

/** Ported 1:1 from main. */
@Data
public class CommentRequest {

    @NotBlank(message = EMPTY_COMMENT)
    private String content;
}
