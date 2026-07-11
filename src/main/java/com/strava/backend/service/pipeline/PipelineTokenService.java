package com.strava.backend.service.pipeline;

import com.strava.backend.domain.PipelineToken;
import com.strava.backend.domain.User;

import java.util.List;
import java.util.Optional;

public interface PipelineTokenService {

    /**
     * Issue a new token for the user. The returned {@link IssuedToken#plaintext()}
     * is the only time the raw token is ever exposed.
     */
    IssuedToken issueToken(Long userId, String name);

    List<PipelineToken> listTokens(Long userId);

    /** Revoke (soft-delete) a token owned by the user. */
    void revokeToken(Long userId, Long tokenId);

    /**
     * Resolve a raw {@code tri_...} bearer token to its owner. Empty when the
     * token is unknown, malformed, or revoked.
     */
    Optional<User> authenticate(String rawToken);

    record IssuedToken(PipelineToken token, String plaintext) {
    }
}
