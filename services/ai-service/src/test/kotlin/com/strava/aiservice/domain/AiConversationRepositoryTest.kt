package com.strava.aiservice.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.dao.DataIntegrityViolationException

/**
 * Runs against H2 (MODE=PostgreSQL) with the real Flyway V1 migration —
 * validates the monolith-parity schema against the entity mapping and ports
 * main's AiConversationRepositoryTest to the logical-id model.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("AiConversationRepository Integration Tests")
class AiConversationRepositoryTest {

    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var conversationRepository: AiConversationRepository

    @Test
    @DisplayName("findByUserIdAndActivityId returns the conversation with its ordered messages")
    fun findByUserIdAndActivityIdReturnsMessages() {
        val conversation = AiConversation(userId = 1, activityId = 100)
        conversation.addMessage(ChatRole.USER, "Analyze this activity")
        conversation.addMessage(ChatRole.ASSISTANT, "Solid aerobic effort at 5:00/km.")
        conversationRepository.saveAndFlush(conversation)
        entityManager.clear()

        val found = conversationRepository.findByUserIdAndActivityId(1, 100)

        assertThat(found).isPresent
        assertThat(found.get().messages).hasSize(2)
        assertThat(found.get().messages[0].role).isEqualTo(ChatRole.USER)
        assertThat(found.get().messages[0].content).isEqualTo("Analyze this activity")
        assertThat(found.get().messages[0].createdAt).isNotNull()
        assertThat(found.get().messages[1].role).isEqualTo(ChatRole.ASSISTANT)
    }

    @Test
    @DisplayName("returns empty when no conversation exists for the (user, activity) pair")
    fun returnsEmptyWhenMissing() {
        assertThat(conversationRepository.findByUserIdAndActivityId(1, 100)).isEmpty
    }

    @Test
    @DisplayName("scopes the lookup to the requesting user")
    fun scopesLookupToUser() {
        conversationRepository.saveAndFlush(AiConversation(userId = 2, activityId = 100))

        assertThat(conversationRepository.findByUserIdAndActivityId(1, 100)).isEmpty
    }

    @Test
    @DisplayName("enforces a single conversation per (user, activity)")
    fun enforcesUniqueConversationPerUserActivity() {
        conversationRepository.saveAndFlush(AiConversation(userId = 1, activityId = 100))

        assertThatThrownBy {
            conversationRepository.saveAndFlush(AiConversation(userId = 1, activityId = 100))
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    @DisplayName("deleting a conversation cascades to its messages")
    fun deleteCascadesToMessages() {
        val conversation = AiConversation(userId = 1, activityId = 100)
        conversation.addMessage(ChatRole.USER, "Hi")
        val saved = conversationRepository.saveAndFlush(conversation)
        entityManager.clear()

        conversationRepository.deleteById(saved.id!!)
        conversationRepository.flush()

        assertThat(entityManager.entityManager.createQuery("SELECT m FROM AiMessage m").resultList).isEmpty()
    }
}
