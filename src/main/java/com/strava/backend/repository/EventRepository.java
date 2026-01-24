package com.strava.backend.repository;

import com.strava.backend.domain.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e WHERE e.isPublic = true AND e.eventDate > :currentDate " +
           "ORDER BY e.eventDate ASC")
    Page<Event> findUpcomingPublicEvents(@Param("currentDate") LocalDateTime currentDate, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.isPublic = true AND e.eventDate > :currentDate " +
           "ORDER BY e.eventDate ASC")
    List<Event> findUpcomingPublicEvents(@Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT e FROM Event e JOIN e.registeredUsers u WHERE u.id = :userId " +
           "AND e.eventDate > :currentDate ORDER BY e.eventDate ASC")
    List<Event> findUpcomingEventsByUserId(
            @Param("userId") Long userId,
            @Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT e FROM Event e WHERE e.organizer.id = :userId ORDER BY e.eventDate DESC")
    List<Event> findByOrganizerId(@Param("userId") Long userId);

    @Query("SELECT e FROM Event e WHERE e.club.id = :clubId AND e.eventDate > :currentDate " +
           "ORDER BY e.eventDate ASC")
    List<Event> findUpcomingEventsByClubId(
            @Param("clubId") Long clubId,
            @Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM Event e JOIN e.registeredUsers u " +
           "WHERE e.id = :eventId AND u.id = :userId")
    boolean isUserRegistered(@Param("eventId") Long eventId, @Param("userId") Long userId);

    @Query("SELECT e FROM Event e WHERE e.city = :city AND e.eventDate > :currentDate " +
           "ORDER BY e.eventDate ASC")
    List<Event> findUpcomingEventsByCity(
            @Param("city") String city,
            @Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT e FROM Event e WHERE " +
           "(UPPER(e.name) LIKE UPPER(CONCAT('%', :searchText, '%')) OR " +
           "UPPER(e.description) LIKE UPPER(CONCAT('%', :searchText, '%')) OR " +
           "UPPER(e.city) LIKE UPPER(CONCAT('%', :searchText, '%'))) " +
           "ORDER BY e.eventDate DESC")
    Page<Event> searchEvents(@Param("searchText") String searchText, Pageable pageable);
}
