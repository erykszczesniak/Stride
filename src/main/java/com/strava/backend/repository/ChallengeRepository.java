package com.strava.backend.repository;

import com.strava.backend.domain.Challenge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    @Query("SELECT c FROM Challenge c WHERE c.isPublic = true " +
           "AND c.startDate <= :currentDate AND c.endDate >= :currentDate " +
           "ORDER BY c.endDate ASC")
    Page<Challenge> findActivePublicChallenges(@Param("currentDate") LocalDateTime currentDate, Pageable pageable);

    @Query("SELECT c FROM Challenge c WHERE c.isPublic = true AND c.startDate > :currentDate " +
           "ORDER BY c.startDate ASC")
    Page<Challenge> findUpcomingPublicChallenges(@Param("currentDate") LocalDateTime currentDate, Pageable pageable);

    @Query("SELECT c FROM Challenge c JOIN c.participants p WHERE p.id = :userId " +
           "AND c.endDate >= :currentDate ORDER BY c.endDate ASC")
    List<Challenge> findActiveChallengesByUserId(
            @Param("userId") Long userId,
            @Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT c FROM Challenge c WHERE c.creator.id = :userId ORDER BY c.createdAt DESC")
    List<Challenge> findByCreatorId(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Challenge c JOIN c.participants p " +
           "WHERE c.id = :challengeId AND p.id = :userId")
    boolean isUserParticipating(@Param("challengeId") Long challengeId, @Param("userId") Long userId);

    @Query("SELECT c FROM Challenge c WHERE " +
           "UPPER(c.name) LIKE UPPER(CONCAT('%', :searchText, '%')) " +
           "ORDER BY c.startDate DESC")
    Page<Challenge> searchChallenges(@Param("searchText") String searchText, Pageable pageable);
}
