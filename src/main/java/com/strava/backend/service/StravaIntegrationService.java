package com.strava.backend.service;

import com.strava.backend.domain.StravaConnection;
import com.strava.backend.dto.strava.StravaStatusResponse;

public interface StravaIntegrationService {

    /** Build the Strava authorization URL with HMAC-signed state for the given user. */
    String buildAuthorizeUrl(Long userId);

    /** Verify state signature and return the embedded user id, or throw. */
    Long verifyStateAndExtractUserId(String state);

    /**
     * Exchange an authorization code for access+refresh tokens and upsert a connection row
     * for {@code userId}. Called from the OAuth callback once state has been validated.
     */
    StravaConnection exchangeCodeAndStoreConnection(Long userId, String code);

    /** Refresh the access token for {@code userId} if it's expired; returns the live token. */
    String refreshTokenIfNeeded(Long userId);

    /** Kick off an asynchronous full-history import for the user. Idempotent by Strava id. */
    void startImportForUser(Long userId);

    StravaStatusResponse getStatus(Long userId);

    void disconnect(Long userId);
}
