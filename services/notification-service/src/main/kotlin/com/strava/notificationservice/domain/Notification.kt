package com.strava.notificationservice.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * Mirrors the monolith's `notifications` table semantics (see main's
 * V7__add_notifications.sql). Differences forced by database-per-service:
 * recipient/sender are logical ids into user-service (no FK), and the sender's
 * first/last name are denormalized onto the row at creation time from the
 * Kafka event's display name — the monolith joined the users table for them.
 */
@Entity
@Table(
    name = "notifications",
    indexes = [
        Index(name = "idx_notifications_recipient_created", columnList = "recipient_id, created_at"),
        Index(name = "idx_notifications_recipient_unread", columnList = "recipient_id, is_read")
    ]
)
class Notification(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "recipient_id", nullable = false)
    var recipientId: Long = 0,

    @Column(name = "sender_id")
    var senderId: Long? = null,

    @Column(name = "sender_first_name", length = 100)
    var senderFirstName: String? = null,

    @Column(name = "sender_last_name", length = 100)
    var senderLastName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var type: NotificationType = NotificationType.SYSTEM,

    @Column(nullable = false, length = 500)
    var message: String = "",

    @Column(name = "reference_id")
    var referenceId: Long? = null,

    @Column(name = "reference_type", length = 50)
    var referenceType: String? = null,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
