package com.strava.challengegoal.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "goals", indexes = @Index(name = "idx_goals_user", columnList = "user_id, period_end"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 32)
    private GoalType goalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GoalPeriod period;

    /** Optional sport filter — null means all sports count toward this goal. */
    @Column(name = "sport_filter", length = 32)
    private String sportFilter;

    /** Target value: km / seconds / meters / count depending on goal_type. */
    @Column(name = "target_value", nullable = false)
    private Double targetValue;

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Goal g)) return false;
        return id != null && id.equals(g.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
