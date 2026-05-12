package com.strava.activityservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "activities", indexes = {
        @Index(name = "idx_activities_user_started", columnList = "user_id, start_time"),
        @Index(name = "idx_activities_user_sport",   columnList = "user_id, activity_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 32)
    private ActivityType activityType;

    /** Distance in kilometers. */
    @Column(nullable = false)
    private Double distance;

    /** Duration in seconds. */
    @Column(nullable = false)
    private Integer duration;

    @Column(name = "elevation_gain")
    private Integer elevationGain;

    private Integer calories;

    @Column(name = "average_speed")
    private Double averageSpeed;

    @Column(name = "max_speed")
    private Double maxSpeed;

    @Column(name = "average_heart_rate")
    private Integer averageHeartRate;

    @Column(name = "max_heart_rate")
    private Integer maxHeartRate;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "start_location")
    private String startLocation;

    @Column(name = "end_location")
    private String endLocation;

    @Column(name = "route_polyline", columnDefinition = "TEXT")
    private String routePolyline;

    @Column(name = "map_image_url")
    private String mapImageUrl;

    @Builder.Default
    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = false;

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

    /** Average pace (seconds per kilometer) using uniform pace from total distance/duration. */
    public double averagePaceSecondsPerKm() {
        if (distance == null || distance <= 0) return 0;
        return duration / distance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Activity a)) return false;
        return id != null && id.equals(a.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
