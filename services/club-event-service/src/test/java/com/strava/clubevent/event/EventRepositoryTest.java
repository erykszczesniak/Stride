package com.strava.clubevent.event;

import com.strava.clubevent.common.ActivityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against H2 with the real Flyway chain (V1 + V2) — validates the
 * reshaped events table and the registered-users element collection JPQL.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("EventRepository Integration Tests")
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    private final LocalDateTime now = LocalDateTime.now();

    private Event upcomingPublic;

    private Event event(String name, LocalDateTime date, boolean isPublic, Set<Long> registered) {
        Event e = Event.builder()
                .name(name)
                .activityType(ActivityType.RUN)
                .location("City Center")
                .eventDate(date)
                .isPublic(isPublic)
                .organizerId(1L)
                .build();
        e.getRegisteredUserIds().addAll(registered);
        return eventRepository.save(e);
    }

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        upcomingPublic = event("City Marathon", now.plusDays(10), true, Set.of(1L, 2L));
        event("Night Run", now.plusDays(3), true, Set.of(2L));
        event("Private Trail", now.plusDays(5), false, Set.of(1L));
        event("Past Race", now.minusDays(10), true, Set.of(1L));
    }

    @Test
    @DisplayName("Should return upcoming public events ordered by date")
    void shouldReturnUpcomingPublicEventsOrderedByDate() {
        Page<Event> result = eventRepository.findUpcomingPublicEvents(now, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Event::getName)
                .containsExactly("Night Run", "City Marathon");
    }

    @Test
    @DisplayName("Should return upcoming events the user registered for")
    void shouldReturnUpcomingEventsByUserId() {
        List<Event> result = eventRepository.findUpcomingEventsByUserId(1L, now);

        assertThat(result).extracting(Event::getName)
                .containsExactly("Private Trail", "City Marathon");
    }

    @Test
    @DisplayName("Should detect registration")
    void shouldDetectRegistration() {
        assertThat(eventRepository.isUserRegistered(upcomingPublic.getId(), 2L)).isTrue();
        assertThat(eventRepository.isUserRegistered(upcomingPublic.getId(), 99L)).isFalse();
    }
}
