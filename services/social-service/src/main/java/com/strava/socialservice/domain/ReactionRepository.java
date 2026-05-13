package com.strava.socialservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    Optional<Reaction> findByActivityIdAndUserIdAndReactionType(
            Long activityId, Long userId, String reactionType);

    long countByActivityIdAndReactionType(Long activityId, String reactionType);

    List<Reaction> findAllByActivityId(Long activityId);

    void deleteByActivityIdAndUserIdAndReactionType(Long activityId, Long userId, String reactionType);
}
