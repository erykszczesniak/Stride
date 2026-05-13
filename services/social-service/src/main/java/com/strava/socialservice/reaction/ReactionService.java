package com.strava.socialservice.reaction;

import com.strava.socialservice.domain.Reaction;
import com.strava.socialservice.domain.ReactionRepository;
import com.strava.socialservice.event.SocialEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReactionService {

    public static final String KUDOS = "KUDOS";

    private final ReactionRepository reactions;
    private final SocialEventPublisher events;

    @Transactional
    public ReactionDto react(Long activityId, Long userId, String reactionType) {
        String type = reactionType == null ? KUDOS : reactionType.toUpperCase();
        return reactions.findByActivityIdAndUserIdAndReactionType(activityId, userId, type)
                .map(ReactionDto::from)
                .orElseGet(() -> {
                    Reaction r = Reaction.builder()
                            .activityId(activityId)
                            .userId(userId)
                            .reactionType(type)
                            .build();
                    reactions.save(r);
                    events.publishReactionAdded(r);
                    return ReactionDto.from(r);
                });
    }

    @Transactional
    public void unreact(Long activityId, Long userId, String reactionType) {
        String type = reactionType == null ? KUDOS : reactionType.toUpperCase();
        reactions.deleteByActivityIdAndUserIdAndReactionType(activityId, userId, type);
    }

    @Transactional(readOnly = true)
    public long count(Long activityId, String reactionType) {
        return reactions.countByActivityIdAndReactionType(activityId, reactionType == null ? KUDOS : reactionType);
    }

    @Transactional(readOnly = true)
    public List<ReactionDto> listForActivity(Long activityId) {
        return reactions.findAllByActivityId(activityId).stream().map(ReactionDto::from).toList();
    }
}
