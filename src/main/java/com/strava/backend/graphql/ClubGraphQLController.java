package com.strava.backend.graphql;

import com.strava.backend.domain.Club;
import com.strava.backend.domain.User;
import com.strava.backend.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ClubGraphQLController {

    private static final Long DEFAULT_USER_ID = 1L;

    private final ClubService clubService;

    @QueryMapping
    public Club club(@Argument Long id) {
        return clubService.getClubById(id);
    }

    @QueryMapping
    public List<Club> clubs(@Argument Integer page, @Argument Integer size) {
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 10;
        return clubService.getAllPublicClubs(PageRequest.of(pageNum, pageSize)).getContent();
    }

    @QueryMapping
    public List<Club> userClubs(@Argument Long userId) {
        return clubService.getClubsByUserId(userId);
    }

    @MutationMapping
    public Boolean joinClub(@Argument Long clubId) {
        clubService.joinClub(clubId, DEFAULT_USER_ID);
        return true;
    }

    @MutationMapping
    public Boolean leaveClub(@Argument Long clubId) {
        clubService.leaveClub(clubId, DEFAULT_USER_ID);
        return true;
    }

    // Schema mappings for computed fields
    @SchemaMapping(typeName = "Club", field = "membersCount")
    public int membersCount(Club club) {
        return club.getMembers() != null ? club.getMembers().size() : 0;
    }

    @SchemaMapping(typeName = "Club", field = "isMember")
    public boolean isMember(Club club) {
        return clubService.isUserMember(club.getId(), DEFAULT_USER_ID);
    }

    @SchemaMapping(typeName = "Club", field = "owner")
    public User owner(Club club) {
        return club.getOwner();
    }
}
