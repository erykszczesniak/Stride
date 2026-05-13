package com.strava.notificationservice.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "notifications",
    indexes = [
        Index(name = "idx_notifications_recipient_created", columnList = "recipient_user_id, created_at"),
        Index(name = "idx_notifications_recipient_unread",  columnList = "recipient_user_id, read")
    ]
)
class Notification(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "recipient_user_id", nullable = false)
    var recipientUserId: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var type: NotificationType = NotificationType.SYSTEM,

    @Column(nullable = false, length = 255)
    var title: String = "",

    @Column(length = 2000)
    var body: String? = null,

    /** Optional JSON-encoded metadata (e.g. {"activityId":"42","sport":"RUN"}). */
    @Column(columnDefinition = "TEXT")
    var metadata: String? = null,

    @Column(nullable = false)
    var read: Boolean = false,

    @Column(name = "read_at")
    var readAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    fun onCreate() {
        if (createdAt == LocalDateTime.MIN) createdAt = LocalDateTime.now()
    }
}
