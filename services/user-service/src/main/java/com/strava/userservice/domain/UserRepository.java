package com.strava.userservice.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
        SELECT u FROM User u
        WHERE u.hideFromSearch = false
          AND (LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(u.email)     LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<User> search(@Param("q") String query, Pageable pageable);
}
