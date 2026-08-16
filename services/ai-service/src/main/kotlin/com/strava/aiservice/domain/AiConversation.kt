package com.strava.aiservice.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/**
 * A single AI activity-analysis conversation, scoped to one user and one activity.
 * There is at most one conversation per (user, activity) pair — enforced by the unique
 * constraint — so re-opening the chat for an activity continues where it left off.
 *
 * Mirrors the monolith's domain/AiConversation with one database-per-service
 * difference: user_id and activity_id are logical references into user-service
 * and activity-service (no cross-schema FKs) instead of @ManyToOne associations.
 */
@Entity
@Table(
    name = "ai_conversations",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_ai_conversation_user_activity",
            columnNames = ["user_id", "activity_id"]
        )
    ]
)
class AiConversation(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Column(name = "activity_id", nullable = false)
    var activityId: Long = 0,

    @OneToMany(mappedBy = "conversation", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("createdAt ASC, id ASC")
    var messages: MutableList<AiMessage> = mutableListOf(),

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
) {

    @PrePersist
    fun onCreate() {
        createdAt = LocalDateTime.now()
        updatedAt = createdAt
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }

    /** Appends a message authored by [role], wiring the back-reference. */
    fun addMessage(role: ChatRole, content: String): AiMessage {
        val message = AiMessage(conversation = this, role = role, content = content)
        messages.add(message)
        return message
    }
}
