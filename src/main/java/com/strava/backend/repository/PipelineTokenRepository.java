package com.strava.backend.repository;

import com.strava.backend.domain.PipelineToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PipelineTokenRepository extends JpaRepository<PipelineToken, Long> {

    // join fetch: the owner is read by the auth filter after the service
    // transaction has closed, so a lazy proxy would blow up there.
    @Query("SELECT t FROM PipelineToken t JOIN FETCH t.user " +
           "WHERE t.tokenHash = :tokenHash AND t.revoked = false")
    Optional<PipelineToken> findByTokenHashAndRevokedFalse(@Param("tokenHash") String tokenHash);

    List<PipelineToken> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<PipelineToken> findByIdAndUserId(Long id, Long userId);
}
