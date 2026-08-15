package com.strava.clubevent.event;

import com.strava.clubevent.common.ActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Mirror of the monolith's Event aggregate. Registered users are plain user
 * ids (logical refs to user-service); the club link stays an id since the
 * public EventResponse never embeds the club.
 */
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

    @Column(name = "organizer_id", nullable = false)
    private Long organizerId;

    @Column(name = "club_id")
    private Long clubId;

    @ElementCollection
    @CollectionTable(name = "event_participants", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "user_id")
    @Builder.Default
    private Set<Long> registeredUserIds = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public int getRegisteredCount() {
        return registeredUserIds != null ? registeredUserIds.size() : 0;
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
