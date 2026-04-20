package com.strava.backend.repository;

import com.strava.backend.domain.StravaImportedActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StravaImportedActivityRepository
        extends JpaRepository<StravaImportedActivity, StravaImportedActivity.PK> {

    boolean existsByStravaActivityIdAndUserId(Long stravaActivityId, Long userId);

    long countByUserId(Long userId);

    @Query("SELECT s.stravaActivityId FROM StravaImportedActivity s WHERE s.userId = :userId")
    List<Long> findStravaActivityIdsByUserId(@Param("userId") Long userId);

    void deleteByUserId(Long userId);
}
