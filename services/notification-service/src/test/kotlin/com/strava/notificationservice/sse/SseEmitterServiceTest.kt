package com.strava.notificationservice.sse

import com.strava.notificationservice.domain.NotificationType
import com.strava.notificationservice.notification.NotificationDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SseEmitterServiceTest {

    private lateinit var service: SseEmitterService

    @BeforeEach
    fun setUp() {
        service = SseEmitterService()
    }

    private fun dto() = NotificationDto(
        id = 5, senderFirstName = "John", senderLastName = "Doe",
        type = NotificationType.LIKE, message = "John Doe liked your activity",
        referenceId = 42, referenceType = "ACTIVITY", read = false,
        createdAt = LocalDateTime.now()
    )

    @Test
    fun `createEmitter registers an active emitter for the user`() {
        val emitter = service.createEmitter(1)

        assertThat(emitter).isNotNull
        assertThat(emitter.timeout).isEqualTo(300_000L)
        assertThat(service.hasActiveEmitter(1)).isTrue()
    }

    @Test
    fun `reconnect replaces the previous emitter`() {
        val first = service.createEmitter(1)
        val second = service.createEmitter(1)

        assertThat(second).isNotSameAs(first)
        assertThat(service.hasActiveEmitter(1)).isTrue()
    }

    @Test
    fun `sendNotification without a subscriber is a no-op`() {
        assertThatCode { service.sendNotification(99, dto()) }.doesNotThrowAnyException()
    }

    @Test
    fun `sendNotification to a subscribed user does not throw`() {
        service.createEmitter(1)

        assertThatCode { service.sendNotification(1, dto()) }.doesNotThrowAnyException()
        assertThat(service.hasActiveEmitter(1)).isTrue()
    }

    @Test
    fun `removeEmitter completes and forgets the emitter`() {
        service.createEmitter(1)

        service.removeEmitter(1)

        assertThat(service.hasActiveEmitter(1)).isFalse()
    }

    @Test
    fun `heartbeats run without error for connected users`() {
        service.createEmitter(1)
        service.createEmitter(2)

        assertThatCode { service.sendHeartbeats() }.doesNotThrowAnyException()
    }
}
