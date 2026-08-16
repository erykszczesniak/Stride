package com.strava.socialservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Local denormalized snapshot of user identity data, populated by the Kafka
 * listeners on user.registered/updated/deleted so responses can embed the
 * author (BaseUserResponse: id/firstName/lastName/avatar) without calling
 * user-service on the read path.
 *
 * The events only carry a displayName ("John Doe"); first/last name are a
 * best-effort split at the first space — same convention as activity-service's
 * user_cache projection.
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

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(length = 500)
    private String avatar;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
