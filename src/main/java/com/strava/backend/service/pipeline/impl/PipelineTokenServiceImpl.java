package com.strava.backend.service.pipeline.impl;

import com.strava.backend.domain.PipelineToken;
import com.strava.backend.domain.User;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.PipelineTokenRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.pipeline.PipelineTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PipelineTokenServiceImpl implements PipelineTokenService {

    static final String TOKEN_PREFIX = "tri_";
    private static final int TOKEN_RANDOM_BYTES = 32;
    /** Skip the last_used_at write unless the previous one is older than this. */
    private static final int LAST_USED_WRITE_INTERVAL_SECONDS = 60;

    private final PipelineTokenRepository pipelineTokenRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public IssuedToken issueToken(Long userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException("User not found", HttpStatus.NOT_FOUND));

        byte[] random = new byte[TOKEN_RANDOM_BYTES];
        secureRandom.nextBytes(random);
        String plaintext = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(random);

        PipelineToken token = PipelineToken.builder()
                .user(user)
                .name(name == null || name.isBlank() ? "API token" : name.trim())
                .tokenHash(sha256Hex(plaintext))
                .tokenSuffix(plaintext.substring(plaintext.length() - 4))
                .build();
        PipelineToken saved = pipelineTokenRepository.save(token);
        log.info("Issued pipeline token {} for user {}", saved.getId(), userId);
        return new IssuedToken(saved, plaintext);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PipelineToken> listTokens(Long userId) {
        return pipelineTokenRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public void revokeToken(Long userId, Long tokenId) {
        PipelineToken token = pipelineTokenRepository.findByIdAndUserId(tokenId, userId)
                .orElseThrow(() -> new ApiRequestException("Token not found", HttpStatus.NOT_FOUND));
        token.setRevoked(true);
        pipelineTokenRepository.save(token);
        log.info("Revoked pipeline token {} for user {}", tokenId, userId);
    }

    @Override
    @Transactional
    public Optional<User> authenticate(String rawToken) {
        if (rawToken == null || !rawToken.startsWith(TOKEN_PREFIX)) {
            return Optional.empty();
        }
        return pipelineTokenRepository.findByTokenHashAndRevokedFalse(sha256Hex(rawToken))
                .map(token -> {
                    touchLastUsed(token);
                    return token.getUser();
                });
    }

    private void touchLastUsed(PipelineToken token) {
        LocalDateTime now = LocalDateTime.now();
        if (token.getLastUsedAt() == null
                || token.getLastUsedAt().plusSeconds(LAST_USED_WRITE_INTERVAL_SECONDS).isBefore(now)) {
            token.setLastUsedAt(now);
            pipelineTokenRepository.save(token);
        }
    }

    static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
