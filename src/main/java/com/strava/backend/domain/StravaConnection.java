package com.strava.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Per-user Strava OAuth2 connection. One row per user (unique FK).
 * Tokens are stored as plain TEXT in this scaffold — encryption-at-rest is explicitly out
 * of scope; production use would add a column-level cipher or pull them from a secret store.
 */
@Entity
@Table(name = "strava_connections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StravaConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "strava_athlete_id")
    private Long stravaAthleteId;

    @JsonIgnore
    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @JsonIgnore
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    private String scope;

    @Column(name = "connected_at", nullable = false, updatable = false)
    private LocalDateTime connectedAt;

    @Column(name = "last_imported_at")
    private LocalDateTime lastImportedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (connectedAt == null) {
            connectedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** True when the access token is either unset or already past its expiry. */
    public boolean isTokenExpired() {
        return tokenExpiresAt == null || tokenExpiresAt.isBefore(LocalDateTime.now());
    }
}
