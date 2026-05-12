package com.strava.activityservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalRecordRepository extends JpaRepository<PersonalRecord, Long> {

    Optional<PersonalRecord> findByUserIdAndActivityTypeAndDistanceType(
            Long userId, ActivityType activityType, DistanceType distanceType);

    List<PersonalRecord> findAllByUserId(Long userId);

    List<PersonalRecord> findAllByUserIdAndActivityType(Long userId, ActivityType activityType);
}
