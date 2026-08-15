package com.strava.challengegoal.goal;

import com.strava.challengegoal.common.ActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Mirror of the monolith's Goal entity — user_id is a logical reference to
 * user-service; the computed progress getters are part of the public contract
 * (progressPercentage / remainingValue / completed on GoalResponse).
 */
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

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (currentValue == null) {
            currentValue = 0.0;
        }
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
