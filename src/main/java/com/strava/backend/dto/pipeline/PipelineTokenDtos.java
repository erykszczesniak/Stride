package com.strava.backend.dto.pipeline;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Request/response shapes for the /api/v1/pipeline/tokens management endpoints. */
public final class PipelineTokenDtos {

    private PipelineTokenDtos() {
    }

    public record CreateTokenRequest(
            @Size(max = 100) String name
    ) {
    }

    /** {@code token} (the plaintext) is only present in the creation response. */
    public record TokenResponse(
            Long id,
            String name,
            @JsonInclude(JsonInclude.Include.NON_NULL) String token,
            String suffix,
            LocalDateTime createdAt,
            LocalDateTime lastUsedAt,
            boolean revoked
    ) {
    }
}
