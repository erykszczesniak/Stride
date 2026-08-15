package com.strava.clubevent.club;

import com.strava.clubevent.common.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClubRepository extends JpaRepository<Club, Long> {

    @Query("SELECT c FROM Club c WHERE c.isPrivate = false ORDER BY SIZE(c.memberIds) DESC")
    Page<Club> findPublicClubsOrderByMembersCount(Pageable pageable);

    @Query("SELECT c FROM Club c JOIN c.memberIds m WHERE m = :userId ORDER BY c.name ASC")
    List<Club> findClubsByUserId(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Club c JOIN c.memberIds m " +
           "WHERE c.id = :clubId AND m = :userId")
    boolean isUserMember(@Param("clubId") Long clubId, @Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Club c JOIN c.adminIds a " +
           "WHERE c.id = :clubId AND a = :userId")
    boolean isUserAdmin(@Param("clubId") Long clubId, @Param("userId") Long userId);

    @Query("SELECT c FROM Club c WHERE " +
           "UPPER(c.name) LIKE UPPER(CONCAT('%', :searchText, '%')) OR " +
           "UPPER(c.city) LIKE UPPER(CONCAT('%', :searchText, '%')) " +
           "ORDER BY SIZE(c.memberIds) DESC")
    Page<Club> searchClubs(@Param("searchText") String searchText, Pageable pageable);

    @Query("SELECT c FROM Club c WHERE c.sportType = :sportType AND c.isPrivate = false " +
           "ORDER BY SIZE(c.memberIds) DESC")
    Page<Club> findPublicClubsBySportType(@Param("sportType") ActivityType sportType, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Club c WHERE c.isPrivate = false")
    long countPublicClubs();

    @Query("SELECT COALESCE(SUM(SIZE(c.memberIds)), 0) FROM Club c WHERE c.isPrivate = false")
    long sumAllMembers();
}
