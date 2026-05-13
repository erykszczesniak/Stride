package com.strava.clubevent.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "club_members", indexes = @Index(name = "idx_club_members_user", columnList = "user_id"))
@IdClass(ClubMemberId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubMember {

    @Id
    @Column(name = "club_id")
    private Long clubId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ClubMemberRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    void onCreate() {
        if (joinedAt == null) joinedAt = LocalDateTime.now();
    }
}
