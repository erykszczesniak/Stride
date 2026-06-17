package com.strava.backend.repository;

import com.strava.backend.domain.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    /**
     * Fetches the conversation for an activity owned by the given user, eagerly loading its
     * messages so the service can map them outside the persistence context.
     */
    @Query("""
            SELECT c FROM AiConversation c
            LEFT JOIN FETCH c.messages
            WHERE c.user.id = :userId AND c.activity.id = :activityId
            """)
    Optional<AiConversation> findByUserIdAndActivityId(@Param("userId") Long userId,
                                                       @Param("activityId") Long activityId);
}
