package com.strava.socialservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Emoji reaction on a comment — ported from main's CommentReaction. The
 * comment is referenced by id (the FK with ON DELETE CASCADE lives in the
 * schema); user_id is a logical reference to user-service.
 */
@Entity
@Table(name = "comment_reactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_comment_reaction",
                columnNames = {"comment_id", "user_id", "emoji"}),
        indexes = {
                @Index(name = "idx_comment_reactions_comment", columnList = "comment_id"),
                @Index(name = "idx_comment_reactions_user",    columnList = "user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String emoji;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommentReaction r)) return false;
        return id != null && id.equals(r.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
