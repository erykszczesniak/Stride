package com.strava.clubevent.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e WHERE e.isPublic = true AND e.eventDate > :currentDate " +
           "ORDER BY e.eventDate ASC")
    Page<Event> findUpcomingPublicEvents(@Param("currentDate") LocalDateTime currentDate, Pageable pageable);

    @Query("SELECT e FROM Event e JOIN e.registeredUserIds u WHERE u = :userId " +
           "AND e.eventDate > :currentDate ORDER BY e.eventDate ASC")
    List<Event> findUpcomingEventsByUserId(
            @Param("userId") Long userId,
            @Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM Event e JOIN e.registeredUserIds u " +
           "WHERE e.id = :eventId AND u = :userId")
    boolean isUserRegistered(@Param("eventId") Long eventId, @Param("userId") Long userId);
}
