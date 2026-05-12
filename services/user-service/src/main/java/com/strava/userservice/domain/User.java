package com.strava.userservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(length = 500)
    private String avatar;

    @Column(length = 1000)
    private String bio;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String country;

    private Double weight;

    private Double height;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    private AuthProvider authProvider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "password_reset_code", length = 255)
    private String passwordResetCode;

    @Column(name = "password_reset_code_expiry")
    private LocalDateTime passwordResetCodeExpiry;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "email_notifications", nullable = false)
    private boolean emailNotifications = true;

    @Builder.Default
    @Column(name = "push_notifications", nullable = false)
    private boolean pushNotifications = true;

    @Builder.Default
    @Column(name = "weekly_summary", nullable = false)
    private boolean weeklySummary = true;

    @Builder.Default
    @Column(name = "is_private", nullable = false)
    private Boolean isPrivate = false;

    @Builder.Default
    @Column(name = "hide_from_search", nullable = false)
    private Boolean hideFromSearch = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (role == null) role = Role.USER;
        if (authProvider == null) authProvider = AuthProvider.LOCAL;
        if (isPrivate == null) isPrivate = false;
        if (hideFromSearch == null) hideFromSearch = false;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
