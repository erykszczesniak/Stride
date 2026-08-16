package com.strava.activityservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Ported from main's Gear entity. The monolith's {@code @ManyToOne User user}
 * becomes a logical {@code userId} column — user identity lives in
 * user-service and crossing service DBs with an FK would couple the schemas.
 */
@Entity
@Table(name = "gear", indexes = {
        @Index(name = "idx_gear_user_id", columnList = "user_id"),
        @Index(name = "idx_gear_user_retired", columnList = "user_id, retired")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Logical reference to user-service.users.id — no FK across service DBs. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    private String brand;

    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GearType type;

    /**
     * Which activity types this gear can be tagged against. Running shoes
     * are limited to RUN/TRAIL_RUN/VIRTUAL_RUN, a road bike to RIDE/VIRTUAL_RIDE,
     * etc. Enforced at service layer when an activity is tagged with gear.
     */
    @ElementCollection(targetClass = ActivityType.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "gear_activity_types",
            joinColumns = @JoinColumn(name = "gear_id"))
    @Column(name = "activity_type", nullable = false)
    @Builder.Default
    private Set<ActivityType> activityTypes = EnumSet.noneOf(ActivityType.class);

    /**
     * Denormalized rolling total of kilometers tagged to this gear. Kept in
     * sync by ActivityService when activities with a gear_id are created,
     * updated, or deleted. Avoids a SUM(distance) aggregation on every list.
     */
    @Column(name = "distance_km_total", nullable = false)
    @Builder.Default
    private double distanceKmTotal = 0.0;

    @Column(name = "activity_count", nullable = false)
    @Builder.Default
    private int activityCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean retired = false;

    @Column(name = "retired_at")
    private LocalDateTime retiredAt;

    @Column(length = 2000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
