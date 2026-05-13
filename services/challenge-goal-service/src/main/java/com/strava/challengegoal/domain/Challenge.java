package com.strava.challengegoal.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "challenges", indexes = {
        @Index(name = "idx_challenges_end_time", columnList = "end_time"),
        @Index(name = "idx_challenges_creator", columnList = "creator_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 32)
    private GoalType goalType;

    @Column(name = "sport_filter", length = 32)
    private String sportFilter;

    /** Target value: km / seconds / meters / count depending on goal_type. */
    @Column(name = "target_value", nullable = false)
    private Double targetValue;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "creator_user_id", nullable = false)
    private Long creatorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ChallengeVisibility visibility;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Challenge c)) return false;
        return id != null && id.equals(c.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
