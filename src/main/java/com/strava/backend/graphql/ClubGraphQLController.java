package com.strava.backend.graphql;

import com.strava.backend.domain.Club;
import com.strava.backend.domain.User;
import com.strava.backend.security.UserPrincipal;
import com.strava.backend.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ClubGraphQLController {

    private final ClubService clubService;

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getId();
        }
        return null;
    }

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
        clubService.joinClub(clubId, getAuthenticatedUserId());
        return true;
    }

    @MutationMapping
    public Boolean leaveClub(@Argument Long clubId) {
        clubService.leaveClub(clubId, getAuthenticatedUserId());
        return true;
    }

    // Schema mappings for computed fields
    @SchemaMapping(typeName = "Club", field = "membersCount")
    public int membersCount(Club club) {
        return club.getMembers() != null ? club.getMembers().size() : 0;
    }

    @SchemaMapping(typeName = "Club", field = "isMember")
    public boolean isMember(Club club) {
        return clubService.isUserMember(club.getId(), getAuthenticatedUserId());
    }

    @SchemaMapping(typeName = "Club", field = "owner")
    public User owner(Club club) {
        return club.getOwner();
    }
}
