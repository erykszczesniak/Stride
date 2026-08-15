package com.strava.activityservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    /** Logical reference to user-service.users.id — no FK across service DBs. */
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

    @Column(name = "average_cadence")
    private Integer averageCadence;

    /** Watts. */
    @Column(name = "average_power")
    private Integer averagePower;

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

    // JSON-serialized array of doubles (meters), one per polyline point.
    // Null when the source had no altitude data — consumers treat null as
    // "no elevation profile available".
    @Column(name = "elevation_profile", columnDefinition = "TEXT")
    private String elevationProfile;

    // JSON-serialized array of longs (epoch seconds), 1:1 aligned with the
    // polyline points. Null when the source had no per-trackpoint time data —
    // consumers fall back to uniform-pace splits, so "no timestamps" degrades
    // gracefully.
    @Column(name = "point_timestamps", columnDefinition = "TEXT")
    private String pointTimestamps;

    @Column(name = "map_image_url")
    private String mapImageUrl;

    @Builder.Default
    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = false;

    @Builder.Default
    @Column(name = "has_kudos", nullable = false)
    private boolean hasKudos = false;

    @ElementCollection
    @CollectionTable(name = "activity_photos", joinColumns = @JoinColumn(name = "activity_id"))
    @Column(name = "photo_url")
    @Builder.Default
    private List<String> photos = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        calculateDerivedFields();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateDerivedFields();
    }

    // Mirrors main's derived-field behavior: averageSpeed is always
    // recomputed from distance/duration on persist (a client-supplied value
    // is overwritten), and endTime defaults to startTime + duration.
    private void calculateDerivedFields() {
        if (distance != null && duration != null && duration > 0) {
            this.averageSpeed = (distance / duration) * 3600; // km/h
        }
        if (endTime == null && startTime != null && duration != null) {
            this.endTime = startTime.plusSeconds(duration);
        }
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
