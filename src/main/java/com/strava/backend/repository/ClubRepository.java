package com.strava.backend.repository;

import com.strava.backend.domain.Club;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {

    @Query("SELECT c FROM Club c WHERE c.isPrivate = false ORDER BY SIZE(c.members) DESC")
    Page<Club> findPublicClubsOrderByMembersCount(Pageable pageable);

    @Query("SELECT c FROM Club c JOIN c.members m WHERE m.id = :userId ORDER BY c.name ASC")
    List<Club> findClubsByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM Club c WHERE c.owner.id = :userId ORDER BY c.createdAt DESC")
    List<Club> findByOwnerId(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Club c JOIN c.members m " +
           "WHERE c.id = :clubId AND m.id = :userId")
    boolean isUserMember(@Param("clubId") Long clubId, @Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Club c JOIN c.admins a " +
           "WHERE c.id = :clubId AND a.id = :userId")
    boolean isUserAdmin(@Param("clubId") Long clubId, @Param("userId") Long userId);

    @Query("SELECT c FROM Club c WHERE " +
           "UPPER(c.name) LIKE UPPER(CONCAT('%', :searchText, '%')) OR " +
           "UPPER(c.city) LIKE UPPER(CONCAT('%', :searchText, '%')) " +
           "ORDER BY SIZE(c.members) DESC")
    Page<Club> searchClubs(@Param("searchText") String searchText, Pageable pageable);

    @Query("SELECT c FROM Club c WHERE c.city = :city AND c.isPrivate = false " +
           "ORDER BY SIZE(c.members) DESC")
    List<Club> findPublicClubsByCity(@Param("city") String city);

    @Query("SELECT c FROM Club c WHERE c.sportType = :sportType AND c.isPrivate = false " +
           "ORDER BY SIZE(c.members) DESC")
    Page<Club> findPublicClubsBySportType(@Param("sportType") com.strava.backend.enums.ActivityType sportType, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Club c WHERE c.isPrivate = false")
    long countPublicClubs();

    @Query("SELECT COALESCE(SUM(SIZE(c.members)), 0) FROM Club c WHERE c.isPrivate = false")
    long sumAllMembers();
}
