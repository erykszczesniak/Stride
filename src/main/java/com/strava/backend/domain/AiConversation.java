package com.strava.backend.domain;

import com.strava.backend.enums.ChatRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A single AI activity-analysis conversation, scoped to one user and one activity.
 * There is at most one conversation per (user, activity) pair — enforced by the unique
 * constraint — so re-opening the chat for an activity continues where it left off.
 */
@Entity
@Table(
        name = "ai_conversations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_ai_conversation_user_activity",
                columnNames = {"user_id", "activity_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC, id ASC")
    @Builder.Default
    private List<AiMessage> messages = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** Appends a message authored by {@code role}, wiring the back-reference. */
    public AiMessage addMessage(ChatRole role, String content) {
        AiMessage message = AiMessage.builder()
                .conversation(this)
                .role(role)
                .content(content)
                .build();
        messages.add(message);
        return message;
    }
}
