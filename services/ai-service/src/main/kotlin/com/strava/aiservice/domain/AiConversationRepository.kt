package com.strava.aiservice.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface AiConversationRepository : JpaRepository<AiConversation, Long> {

    /**
     * Fetches the conversation for an activity owned by the given user, eagerly loading its
     * messages so the service can map them outside the persistence context.
     * Same JPQL as the monolith's AiConversationRepository, adapted to the
     * logical-id columns (c.userId / c.activityId instead of associations).
     */
    @Query(
        """
        SELECT c FROM AiConversation c
        LEFT JOIN FETCH c.messages
        WHERE c.userId = :userId AND c.activityId = :activityId
        """
    )
    fun findByUserIdAndActivityId(
        @Param("userId") userId: Long,
        @Param("activityId") activityId: Long
    ): Optional<AiConversation>
}
