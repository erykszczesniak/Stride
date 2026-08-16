package com.strava.pipeline.token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against H2 with the real Flyway chain (V1) — validates the
 * pipeline_tokens table (mirror of main's V18) against the entity mapping,
 * and the revoked-token filter the auth filter depends on.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("PipelineTokenRepository Integration Tests")
class PipelineTokenRepositoryTest {

    @Autowired
    private PipelineTokenRepository repository;

    private PipelineToken save(long userId, String hash, boolean revoked, LocalDateTime createdAt) {
        return repository.save(PipelineToken.builder()
                .userId(userId)
                .name("token")
                .tokenHash(hash)
                .tokenSuffix("7f2a")
                .createdAt(createdAt)
                .revoked(revoked)
                .build());
    }

    @Test
    @DisplayName("hash lookup only resolves live tokens")
    void hashLookupSkipsRevoked() {
        save(7L, "live-hash", false, LocalDateTime.now());
        save(7L, "revoked-hash", true, LocalDateTime.now());

        Optional<PipelineToken> live = repository.findByTokenHashAndRevokedFalse("live-hash");
        assertThat(live).isPresent();
        assertThat(live.get().getUserId()).isEqualTo(7L);

        assertThat(repository.findByTokenHashAndRevokedFalse("revoked-hash")).isEmpty();
        assertThat(repository.findByTokenHashAndRevokedFalse("unknown")).isEmpty();
    }

    @Test
    @DisplayName("listing is per user, newest first, and includes revoked tokens")
    void listingOrdersNewestFirst() {
        save(7L, "h1", false, LocalDateTime.now().minusDays(2));
        save(7L, "h2", true, LocalDateTime.now());
        save(8L, "h3", false, LocalDateTime.now());

        var tokens = repository.findByUserIdOrderByCreatedAtDesc(7L);

        assertThat(tokens).hasSize(2);
        assertThat(tokens.get(0).getTokenHash()).isEqualTo("h2");
        assertThat(tokens.get(1).getTokenHash()).isEqualTo("h1");
    }

    @Test
    @DisplayName("ownership-scoped lookup hides foreign tokens")
    void ownershipScopedLookup() {
        PipelineToken mine = save(7L, "h1", false, LocalDateTime.now());

        assertThat(repository.findByIdAndUserId(mine.getId(), 7L)).isPresent();
        assertThat(repository.findByIdAndUserId(mine.getId(), 8L)).isEmpty();
    }

    @Test
    @DisplayName("created_at is filled by the entity when absent")
    void prePersistFillsCreatedAt() {
        PipelineToken token = repository.save(PipelineToken.builder()
                .userId(7L)
                .name("token")
                .tokenHash("h9")
                .tokenSuffix("7f2a")
                .build());

        assertThat(token.getCreatedAt()).isNotNull();
    }
}
