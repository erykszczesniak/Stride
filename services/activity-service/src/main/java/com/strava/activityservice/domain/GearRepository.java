package com.strava.activityservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GearRepository extends JpaRepository<Gear, Long> {

    List<Gear> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Gear> findByUserIdAndRetiredFalseOrderByCreatedAtDesc(Long userId);
}
