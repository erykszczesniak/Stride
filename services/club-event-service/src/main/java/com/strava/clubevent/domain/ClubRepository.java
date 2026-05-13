package com.strava.clubevent.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubRepository extends JpaRepository<Club, Long> {

    @Query("""
        SELECT c FROM Club c
        WHERE c.isPrivate = false
          AND (:q IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Club> searchPublic(@Param("q") String query, Pageable pageable);
}
