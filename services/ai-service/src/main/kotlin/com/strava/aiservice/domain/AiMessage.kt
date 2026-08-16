package com.strava.aiservice.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * One turn in an [AiConversation] — either the user's prompt or Claude's reply.
 * Mirrors the monolith's domain/AiMessage.
 */
@Entity
@Table(name = "ai_messages")
class AiMessage(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    var conversation: AiConversation? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: ChatRole = ChatRole.USER,

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String = "",

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
) {

    @PrePersist
    fun onCreate() {
        createdAt = LocalDateTime.now()
    }
}
