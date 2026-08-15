package com.strava.userservice.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Look up a user by their external OAuth provider identity. Used by the Strava login
     * flow where no email is available — Strava's default scope only exposes the athlete id.
     */
    Optional<User> findByProviderIdAndAuthProvider(String providerId, AuthProvider authProvider);

    Optional<User> findByPasswordResetCode(String code);

    @Query("SELECT u FROM User u WHERE u.id IN :ids")
    List<User> findByIdIn(@Param("ids") List<Long> ids);

    /**
     * SQL {@code ILIKE} search retained as the fallback for when Elasticsearch is
     * unreachable. Same predicate as the monolith's {@code searchUsersByText}.
     */
    @Query("""
        SELECT u FROM User u
        WHERE (u.hideFromSearch = false OR u.hideFromSearch IS NULL)
          AND (UPPER(u.firstName) LIKE UPPER(CONCAT('%', :searchText, '%'))
            OR UPPER(u.lastName)  LIKE UPPER(CONCAT('%', :searchText, '%'))
            OR UPPER(u.email)     LIKE UPPER(CONCAT('%', :searchText, '%')))
        """)
    Page<User> searchUsersByText(@Param("searchText") String searchText, Pageable pageable);

    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Suggestion feed: everyone the user does not already follow (excluding themselves),
     * most-followed first — port of the monolith's {@code findSuggestedUsers} rewritten
     * against the {@link UserFollow} join entity.
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.id <> :userId
          AND u.id NOT IN (SELECT f.userId FROM UserFollow f WHERE f.followerId = :userId)
        ORDER BY (SELECT COUNT(f2) FROM UserFollow f2 WHERE f2.userId = u.id) DESC, u.id ASC
        """)
    List<User> findSuggestedUsers(@Param("userId") Long userId, Pageable pageable);

    /** Recipients for the weekly summary e-mail digest (consumed by notification-service). */
    Page<User> findByWeeklySummaryTrueAndActiveTrue(Pageable pageable);
}
