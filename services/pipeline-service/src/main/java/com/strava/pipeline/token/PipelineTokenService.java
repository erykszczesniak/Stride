package com.strava.pipeline.token;

import java.util.List;
import java.util.Optional;

public interface PipelineTokenService {

    /** Plaintext prefix of every personal pipeline token. */
    String TOKEN_PREFIX = "tri_";

    /**
     * Issue a new token for the user. The returned {@link IssuedToken#plaintext()}
     * is the only time the raw token is ever exposed.
     */
    IssuedToken issueToken(Long userId, String name);

    List<PipelineToken> listTokens(Long userId);

    /** Revoke (soft-delete) a token owned by the user. */
    void revokeToken(Long userId, Long tokenId);

    /**
     * Resolve a raw {@code tri_...} bearer token to its owner's user id.
     * Empty when the token is unknown, malformed, or revoked.
     */
    Optional<Long> authenticate(String rawToken);

    record IssuedToken(PipelineToken token, String plaintext) {
    }
}
