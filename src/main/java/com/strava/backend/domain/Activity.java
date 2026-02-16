package com.strava.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.strava.backend.enums.ActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType activityType;

    @Column(nullable = false)
    private Double distance; // in kilometers

    @Column(nullable = false)
    private Integer duration; // in seconds

    private Integer elevationGain; // in meters

    private Integer calories;

    private Double averageSpeed; // km/h

    private Double maxSpeed; // km/h

    private Integer averageHeartRate;

    private Integer maxHeartRate;

    private Integer averageCadence;

    private Integer averagePower; // watts

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String startLocation;

    private String endLocation;

    @Column(columnDefinition = "TEXT")
    private String routePolyline;

    private String mapImageUrl;

    private boolean isPrivate;

    private boolean hasKudos;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "activity_likes",
            joinColumns = @JoinColumn(name = "activity_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> likedBy = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Comment> comments = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        calculateDerivedFields();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateDerivedFields();
    }

    private void calculateDerivedFields() {
        if (distance != null && duration != null && duration > 0) {
            this.averageSpeed = (distance / duration) * 3600; // km/h
        }
        if (endTime == null && startTime != null && duration != null) {
            this.endTime = startTime.plusSeconds(duration);
        }
    }

    public int getLikesCount() {
        return likedBy != null ? likedBy.size() : 0;
    }

    public int getCommentsCount() {
        return comments != null ? comments.size() : 0;
    }
}
