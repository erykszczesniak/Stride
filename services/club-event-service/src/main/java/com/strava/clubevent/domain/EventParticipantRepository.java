package com.strava.clubevent.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, EventParticipantId> {

    Page<EventParticipant> findAllByEventId(Long eventId, Pageable pageable);

    List<EventParticipant> findAllByUserId(Long userId);

    long countByEventIdAndStatus(Long eventId, ParticipantStatus status);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    void deleteByEventIdAndUserId(Long eventId, Long userId);
}
