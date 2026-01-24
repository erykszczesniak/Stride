package com.strava.backend.domain;

import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.GoalPeriod;
import com.strava.backend.enums.GoalType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalType goalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalPeriod period;

    @Enumerated(EnumType.STRING)
    private ActivityType activityType;

    @Column(nullable = false)
    private Double targetValue; // distance in km, time in hours, elevation in meters, or count

    private Double currentValue;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Builder.Default
    private boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (currentValue == null) {
            currentValue = 0.0;
        }
        // isActive defaults to true via @Builder.Default, can be overridden when needed
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public double getProgressPercentage() {
        if (targetValue == null || targetValue == 0) {
            return 0.0;
        }
        double progress = (currentValue != null ? currentValue : 0.0) / targetValue * 100;
        return Math.min(progress, 100.0);
    }

    public double getRemainingValue() {
        double current = currentValue != null ? currentValue : 0.0;
        return Math.max(targetValue - current, 0.0);
    }

    public boolean isCompleted() {
        return currentValue != null && currentValue >= targetValue;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endDate);
    }
}
