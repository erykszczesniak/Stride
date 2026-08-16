package com.strava.pipeline.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PipelineTokenRepository extends JpaRepository<PipelineToken, Long> {

    Optional<PipelineToken> findByTokenHashAndRevokedFalse(String tokenHash);

    List<PipelineToken> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<PipelineToken> findByIdAndUserId(Long id, Long userId);
}
