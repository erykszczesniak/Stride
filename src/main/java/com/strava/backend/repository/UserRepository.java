package com.strava.backend.repository;

import com.strava.backend.domain.User;
import com.strava.backend.enums.AuthProvider;
import com.strava.backend.repository.projection.UserProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Look up a user by their external OAuth provider identity. Used by the Strava login
     * flow where no email is available — Strava's default scope only exposes the athlete id.
     */
    Optional<User> findByProviderIdAndAuthProvider(String providerId, AuthProvider authProvider);

    Optional<User> findByPasswordResetCode(String code);

    @Query("SELECT u FROM User u ORDER BY u.id ASC")
    Page<UserProjection> findAllByOrderByIdAsc(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.id IN :ids")
    List<User> findByIdIn(@Param("ids") List<Long> ids);

    @Query("SELECT u FROM User u WHERE " +
           "(u.hideFromSearch = false OR u.hideFromSearch IS NULL) AND (" +
           "UPPER(u.firstName) LIKE UPPER(CONCAT('%', :searchText, '%')) OR " +
           "UPPER(u.lastName) LIKE UPPER(CONCAT('%', :searchText, '%')) OR " +
           "UPPER(u.email) LIKE UPPER(CONCAT('%', :searchText, '%')))")
    Page<UserProjection> searchUsers(@Param("searchText") String searchText, Pageable pageable);

    @Query("SELECT u FROM User u WHERE " +
           "(u.hideFromSearch = false OR u.hideFromSearch IS NULL) AND (" +
           "UPPER(u.firstName) LIKE UPPER(CONCAT('%', :searchText, '%')) OR " +
           "UPPER(u.lastName) LIKE UPPER(CONCAT('%', :searchText, '%')) OR " +
           "UPPER(u.email) LIKE UPPER(CONCAT('%', :searchText, '%')))")
    Page<User> searchUsersByText(@Param("searchText") String searchText, Pageable pageable);

    @Query("SELECT COUNT(f) FROM User u JOIN u.followers f WHERE u.id = :userId")
    int countFollowers(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM User u JOIN u.following f WHERE u.id = :userId")
    int countFollowing(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM User u JOIN u.followers f " +
           "WHERE u.id = :userId AND f.id = :followerId")
    boolean isFollowing(@Param("userId") Long userId, @Param("followerId") Long followerId);

    long countByActive(boolean active);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countUsersCreatedSince(@Param("since") LocalDateTime since);

    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.id != :userId AND u.id NOT IN " +
           "(SELECT f.id FROM User currentUser JOIN currentUser.following f WHERE currentUser.id = :userId) " +
           "ORDER BY SIZE(u.followers) DESC")
    List<User> findSuggestedUsers(@Param("userId") Long userId, Pageable pageable);
}
