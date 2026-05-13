package com.strava.clubevent.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "clubs", indexes = @Index(name = "idx_clubs_owner", columnList = "owner_user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(length = 255)
    private String location;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Builder.Default
    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = false;

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
        if (!(o instanceof Club c)) return false;
        return id != null && id.equals(c.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
