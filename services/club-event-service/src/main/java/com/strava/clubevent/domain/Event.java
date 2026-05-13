package com.strava.clubevent.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_events_start_time", columnList = "start_time"),
        @Index(name = "idx_events_club",       columnList = "club_id"),
        @Index(name = "idx_events_organizer",  columnList = "organizer_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private EventType eventType;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(length = 255)
    private String location;

    @Column(name = "club_id")
    private Long clubId;

    @Column(name = "organizer_user_id", nullable = false)
    private Long organizerUserId;

    @Column(name = "max_participants")
    private Integer maxParticipants;

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
        if (!(o instanceof Event e)) return false;
        return id != null && id.equals(e.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
