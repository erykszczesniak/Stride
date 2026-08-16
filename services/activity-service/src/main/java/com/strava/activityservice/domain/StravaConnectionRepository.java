package com.strava.activityservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StravaConnectionRepository extends JpaRepository<StravaConnection, Long> {

    Optional<StravaConnection> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
