package com.strava.challengegoal.challenge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    @Query("SELECT c FROM Challenge c WHERE c.isPublic = true " +
           "AND c.startDate <= :currentDate AND c.endDate >= :currentDate " +
           "ORDER BY c.endDate ASC")
    Page<Challenge> findActivePublicChallenges(@Param("currentDate") LocalDateTime currentDate, Pageable pageable);

    @Query("SELECT c FROM Challenge c WHERE c.isPublic = true AND c.startDate > :currentDate " +
           "ORDER BY c.startDate ASC")
    Page<Challenge> findUpcomingPublicChallenges(@Param("currentDate") LocalDateTime currentDate, Pageable pageable);

    @Query("SELECT c FROM Challenge c JOIN c.participantIds p WHERE p = :userId " +
           "AND c.endDate >= :currentDate ORDER BY c.endDate ASC")
    List<Challenge> findActiveChallengesByUserId(
            @Param("userId") Long userId,
            @Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Challenge c JOIN c.participantIds p " +
           "WHERE c.id = :challengeId AND p = :userId")
    boolean isUserParticipating(@Param("challengeId") Long challengeId, @Param("userId") Long userId);

    @Query("SELECT c FROM Challenge c WHERE " +
           "UPPER(c.name) LIKE UPPER(CONCAT('%', :searchText, '%')) " +
           "ORDER BY c.startDate DESC")
    Page<Challenge> searchChallenges(@Param("searchText") String searchText, Pageable pageable);

    @Query("SELECT p FROM Challenge c JOIN c.participantIds p WHERE c.id = :challengeId")
    Page<Long> findParticipantIdsByChallengeId(@Param("challengeId") Long challengeId, Pageable pageable);
}
