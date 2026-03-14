package com.strava.backend.graphql;

import com.strava.backend.domain.Event;
import com.strava.backend.security.UserPrincipal;
import com.strava.backend.service.EventService;
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
public class EventGraphQLController {

    private final EventService eventService;

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getId();
        }
        return null;
    }

    @QueryMapping
    public Event event(@Argument Long id) {
        return eventService.getEventById(id);
    }

    @QueryMapping
    public List<Event> upcomingEvents(@Argument Integer page, @Argument Integer size) {
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 10;
        return eventService.getUpcomingPublicEvents(PageRequest.of(pageNum, pageSize)).getContent();
    }

    @QueryMapping
    public List<Event> userEvents(@Argument Long userId) {
        return eventService.getUpcomingEventsByUserId(userId);
    }

    @MutationMapping
    public Boolean registerForEvent(@Argument Long eventId) {
        eventService.registerForEvent(eventId, getAuthenticatedUserId());
        return true;
    }

    @MutationMapping
    public Boolean unregisterFromEvent(@Argument Long eventId) {
        eventService.unregisterFromEvent(eventId, getAuthenticatedUserId());
        return true;
    }

    // Schema mappings for computed fields
    @SchemaMapping(typeName = "Event", field = "registeredCount")
    public int registeredCount(Event event) {
        return event.getRegisteredUsers() != null ? event.getRegisteredUsers().size() : 0;
    }

    @SchemaMapping(typeName = "Event", field = "isRegistered")
    public boolean isRegistered(Event event) {
        return eventService.isUserRegistered(event.getId(), getAuthenticatedUserId());
    }
}
