package com.strava.socialservice.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ported 1:1 from main. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReactionRequest {

    @NotBlank(message = "Emoji is required")
    @Size(max = 10, message = "Emoji must be at most 10 characters")
    private String emoji;
}
