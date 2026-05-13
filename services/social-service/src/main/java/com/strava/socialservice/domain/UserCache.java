package com.strava.socialservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Local denormalized snapshot of user identity data. Populated by
 * UserRegisteredEvent + UserUpdatedEvent consumers so the social feed can
 * render author names/avatars without calling out to user-service on the
 * read path.
 */
@Entity
@Table(name = "user_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCache {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(length = 500)
    private String avatar;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
