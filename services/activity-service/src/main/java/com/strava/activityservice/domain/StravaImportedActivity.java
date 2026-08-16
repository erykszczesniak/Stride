package com.strava.activityservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Dedupe ledger of Strava activity IDs already imported for a given user.
 * Kept out of the Activity entity to avoid polluting rows that never came from Strava.
 */
@Entity
@Table(name = "strava_imported_activities")
@IdClass(StravaImportedActivity.PK.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StravaImportedActivity {

    @Id
    @Column(name = "strava_activity_id")
    private Long stravaActivityId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    @PrePersist
    protected void onCreate() {
        if (importedAt == null) {
            importedAt = LocalDateTime.now();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PK implements Serializable {
        private Long stravaActivityId;
        private Long userId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(stravaActivityId, pk.stravaActivityId)
                    && Objects.equals(userId, pk.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stravaActivityId, userId);
        }
    }
}
