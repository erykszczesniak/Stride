package com.strava.pipeline.token;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Long-lived personal API token granting read-only access to the pipeline
 * facade ({@code /pipeline/v1/**}). Only the SHA-256 hash of the token is
 * persisted — the plaintext ({@code tri_...}) is returned once at creation
 * and cannot be recovered afterwards.
 *
 * Ported from main; {@code user_id} is a logical reference to
 * user-service.users.id (no FK — the user lives in another service's DB).
 */
@Entity
@Table(name = "pipeline_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @JsonIgnore
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /** Last characters of the plaintext token, kept for display ("••••7f2a"). */
    @Column(name = "token_suffix", nullable = false, length = 8)
    private String tokenSuffix;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(nullable = false)
    private boolean revoked;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
