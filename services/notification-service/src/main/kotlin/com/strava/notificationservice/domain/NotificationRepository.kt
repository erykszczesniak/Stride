package com.strava.notificationservice.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

interface NotificationRepository : JpaRepository<Notification, Long> {

    fun findAllByRecipientUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Notification>

    fun countByRecipientUserIdAndReadFalse(userId: Long): Long

    @Modifying
    @Transactional
    @Query("""
        UPDATE Notification n
        SET n.read = true, n.readAt = :now
        WHERE n.recipientUserId = :userId AND n.read = false
        """)
    fun markAllReadForUser(@Param("userId") userId: Long, @Param("now") now: LocalDateTime): Int
}
