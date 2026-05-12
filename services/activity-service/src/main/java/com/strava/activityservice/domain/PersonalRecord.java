package com.strava.activityservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "personal_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_sport_distance",
                columnNames = {"user_id", "activity_type", "distance_type"}),
        indexes = @Index(name = "idx_pr_user_sport", columnList = "user_id, activity_type"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 32)
    private ActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "distance_type", nullable = false, length = 32)
    private DistanceType distanceType;

    @Column(name = "time_in_seconds", nullable = false)
    private Integer timeInSeconds;

    @Column(nullable = false)
    private Double distance;

    @Column(name = "achieved_at", nullable = false)
    private LocalDateTime achievedAt;

    @Column(name = "previous_time_in_seconds")
    private Integer previousTimeInSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersonalRecord p)) return false;
        return id != null && id.equals(p.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
