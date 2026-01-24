package com.strava.backend.domain;

import com.strava.backend.enums.ActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    private String coverImage;

    @Enumerated(EnumType.STRING)
    private ActivityType activityType;

    @Column(nullable = false)
    private LocalDateTime eventDate;

    private LocalDateTime registrationDeadline;

    @Column(nullable = false)
    private String location;

    private String city;

    private String country;

    private Double distance; // in kilometers

    private Double latitude;

    private Double longitude;

    private Integer maxParticipants;

    private Double entryFee;

    private String currency;

    private String externalUrl;

    private boolean isPublic;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    @ManyToMany
    @JoinTable(
            name = "event_participants",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> registeredUsers = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public int getRegisteredCount() {
        return registeredUsers != null ? registeredUsers.size() : 0;
    }

    public boolean isFull() {
        return maxParticipants != null && getRegisteredCount() >= maxParticipants;
    }

    public boolean isRegistrationOpen() {
        if (registrationDeadline == null) {
            return LocalDateTime.now().isBefore(eventDate);
        }
        return LocalDateTime.now().isBefore(registrationDeadline);
    }

    public boolean hasStarted() {
        return LocalDateTime.now().isAfter(eventDate);
    }
}
