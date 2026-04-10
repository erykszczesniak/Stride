package com.strava.backend.repository;

import com.strava.backend.domain.PersonalRecord;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.DistanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalRecordRepository extends JpaRepository<PersonalRecord, Long> {

    @Query("SELECT pr FROM PersonalRecord pr WHERE pr.user.id = :userId ORDER BY pr.distanceType ASC")
    List<PersonalRecord> findByUserId(@Param("userId") Long userId);

    @Query("SELECT pr FROM PersonalRecord pr WHERE pr.user.id = :userId AND pr.activityType = :activityType ORDER BY pr.distanceType ASC")
    List<PersonalRecord> findByUserIdAndActivityType(@Param("userId") Long userId,
                                                     @Param("activityType") ActivityType activityType);

    Optional<PersonalRecord> findByUserIdAndActivityTypeAndDistanceType(Long userId, ActivityType activityType,
                                                                        DistanceType distanceType);

    @Query("SELECT pr FROM PersonalRecord pr WHERE pr.user.id = :userId AND pr.achievedAt >= :since ORDER BY pr.achievedAt DESC")
    List<PersonalRecord> findRecentByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    void deleteByUserId(Long userId);
}
