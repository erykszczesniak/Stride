package com.strava.backend.graphql;

import com.strava.backend.domain.Challenge;
import com.strava.backend.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChallengeGraphQLController {

    private static final Long DEFAULT_USER_ID = 1L;

    private final ChallengeService challengeService;

    @QueryMapping
    public Challenge challenge(@Argument Long id) {
        return challengeService.getChallengeById(id);
    }

    @QueryMapping
    public List<Challenge> activeChallenges(@Argument Integer page, @Argument Integer size) {
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 10;
        return challengeService.getActiveChallenges(PageRequest.of(pageNum, pageSize)).getContent();
    }

    @QueryMapping
    public List<Challenge> userChallenges(@Argument Long userId) {
        return challengeService.getUserActiveChallenges(userId);
    }

    @MutationMapping
    public Boolean joinChallenge(@Argument Long challengeId) {
        challengeService.joinChallenge(challengeId, DEFAULT_USER_ID);
        return true;
    }

    @MutationMapping
    public Boolean leaveChallenge(@Argument Long challengeId) {
        challengeService.leaveChallenge(challengeId, DEFAULT_USER_ID);
        return true;
    }

    // Schema mappings for computed fields
    @SchemaMapping(typeName = "Challenge", field = "participantsCount")
    public int participantsCount(Challenge challenge) {
        return challenge.getParticipants() != null ? challenge.getParticipants().size() : 0;
    }

    @SchemaMapping(typeName = "Challenge", field = "isParticipating")
    public boolean isParticipating(Challenge challenge) {
        return challengeService.isUserParticipating(challenge.getId(), DEFAULT_USER_ID);
    }

    @SchemaMapping(typeName = "Challenge", field = "isActive")
    public boolean isActive(Challenge challenge) {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(challenge.getStartDate()) && !now.isAfter(challenge.getEndDate());
    }

    @SchemaMapping(typeName = "Challenge", field = "userProgress")
    public Double userProgress(Challenge challenge) {
        return challengeService.getUserProgressInChallenge(challenge.getId(), DEFAULT_USER_ID);
    }
}
