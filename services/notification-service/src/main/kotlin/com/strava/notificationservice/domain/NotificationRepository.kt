package com.strava.notificationservice.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NotificationRepository : JpaRepository<Notification, Long> {

    fun findByRecipientIdOrderByCreatedAtDesc(recipientId: Long): List<Notification>

    fun countByRecipientIdAndIsReadFalse(recipientId: Long): Long

    /** Duplicate-suppression rule for unread LIKEs — mirrors the monolith. */
    fun existsByRecipientIdAndSenderIdAndTypeAndReferenceIdAndIsReadFalse(
        recipientId: Long,
        senderId: Long,
        type: NotificationType,
        referenceId: Long
    ): Boolean

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientId = :recipientId AND n.isRead = false")
    fun markAllAsReadByRecipientId(@Param("recipientId") recipientId: Long)

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipientId = :recipientId")
    fun deleteAllByRecipientId(@Param("recipientId") recipientId: Long)
}
