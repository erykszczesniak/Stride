package com.strava.clubevent.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findAllByStartTimeAfterOrderByStartTimeAsc(LocalDateTime now, Pageable pageable);

    Page<Event> findAllByClubIdOrderByStartTimeAsc(Long clubId, Pageable pageable);

    Page<Event> findAllByOrganizerUserIdOrderByStartTimeDesc(Long userId, Pageable pageable);
}
