package com.strava.clubevent.club;

import com.strava.clubevent.common.ActivityType;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Mirror of the monolith's Club aggregate. Members/admins are plain user ids
 * (logical refs to user-service) instead of ManyToMany to a local User table —
 * names/avatars are resolved on read via UserBatchClient.
 */
@Entity
@Table(name = "clubs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    private String coverImage;

    private String profileImage;

    private String city;

    private String country;

    @Enumerated(EnumType.STRING)
    private ActivityType sportType;

    private String website;

    private boolean isPrivate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /**
     * Loaded eagerly (in batches) because {@link #getMembersCount()} is read by the
     * controller after the read-only transaction has ended; a lazy collection there
     * throws LazyInitializationException, which the error dispatch turned into a 403.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @BatchSize(size = 100)
    @CollectionTable(name = "club_members", joinColumns = @JoinColumn(name = "club_id"))
    @Column(name = "user_id")
    @Builder.Default
    private Set<Long> memberIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "club_admins", joinColumns = @JoinColumn(name = "club_id"))
    @Column(name = "user_id")
    @Builder.Default
    private Set<Long> adminIds = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public int getMembersCount() {
        return memberIds != null ? memberIds.size() : 0;
    }
}
