package com.strava.activityservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Local denormalized snapshot of user identity data, populated by the Kafka
 * listeners on user.registered/updated/deleted. Activity responses embed the
 * author (id/firstName/lastName/avatar) from this projection instead of
 * calling user-service per row on the read path.
 *
 * UserUpdatedEvent only carries displayName + avatar, so first/last name are
 * a best-effort split of the display name (text before the first space vs the
 * rest) — good enough for the "John Doe"-style names the product uses.
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
