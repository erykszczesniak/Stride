package com.strava.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.DistanceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "personal_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_activity_type_distance",
                columnNames = {"user_id", "activity_type", "distance_type"}
        ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "distance_type", nullable = false)
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
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
