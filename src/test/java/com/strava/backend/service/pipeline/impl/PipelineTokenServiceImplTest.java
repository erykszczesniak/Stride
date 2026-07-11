package com.strava.backend.service.pipeline.impl;

import com.strava.backend.domain.PipelineToken;
import com.strava.backend.domain.User;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.PipelineTokenRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.pipeline.PipelineTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("PipelineTokenServiceImpl")
class PipelineTokenServiceImplTest {

    @Mock
    private PipelineTokenRepository pipelineTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PipelineTokenServiceImpl service;

    private final User user = User.builder().id(7L).email("user@test.dev").build();

    @Test
    @DisplayName("issues a tri_ token and stores only its SHA-256 hash")
    void issueTokenStoresHashOnly() {
        given(userRepository.findById(7L)).willReturn(Optional.of(user));
        given(pipelineTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        PipelineTokenService.IssuedToken issued = service.issueToken(7L, "iPhone");

        assertThat(issued.plaintext()).startsWith("tri_").hasSizeGreaterThan(30);
        assertThat(issued.token().getName()).isEqualTo("iPhone");
        assertThat(issued.token().getTokenHash())
                .isEqualTo(PipelineTokenServiceImpl.sha256Hex(issued.plaintext()))
                .isNotEqualTo(issued.plaintext())
                .hasSize(64);
        assertThat(issued.token().getTokenSuffix())
                .isEqualTo(issued.plaintext().substring(issued.plaintext().length() - 4));
    }

    @Test
    @DisplayName("authenticates a raw token by hash lookup")
    void authenticateRoundTrip() {
        String plaintext = "tri_someRandomTokenValue1234";
        PipelineToken stored = PipelineToken.builder()
                .id(1L)
                .user(user)
                .tokenHash(PipelineTokenServiceImpl.sha256Hex(plaintext))
                .build();
        given(pipelineTokenRepository.findByTokenHashAndRevokedFalse(stored.getTokenHash()))
                .willReturn(Optional.of(stored));

        Optional<User> authenticated = service.authenticate(plaintext);

        assertThat(authenticated).contains(user);
        then(pipelineTokenRepository).should().save(stored); // last_used_at touch
    }

    @Test
    @DisplayName("rejects tokens without the tri_ prefix without touching the database")
    void rejectsForeignTokens() {
        assertThat(service.authenticate("eyJhbGciOiJIUzI1NiJ9.jwt")).isEmpty();
        assertThat(service.authenticate(null)).isEmpty();
        verifyNoInteractions(pipelineTokenRepository);
    }

    @Test
    @DisplayName("unknown or revoked tokens do not authenticate")
    void unknownTokenIsRejected() {
        given(pipelineTokenRepository.findByTokenHashAndRevokedFalse(any())).willReturn(Optional.empty());

        assertThat(service.authenticate("tri_unknown")).isEmpty();
    }

    @Test
    @DisplayName("revoking another user's token 404s instead of leaking its existence")
    void revokeChecksOwnership() {
        given(pipelineTokenRepository.findByIdAndUserId(9L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.revokeToken(7L, 9L))
                .isInstanceOf(ApiRequestException.class);
    }

    @Test
    @DisplayName("revoke flips the flag and persists")
    void revokePersists() {
        PipelineToken token = PipelineToken.builder().id(9L).user(user).build();
        given(pipelineTokenRepository.findByIdAndUserId(9L, 7L)).willReturn(Optional.of(token));

        service.revokeToken(7L, 9L);

        ArgumentCaptor<PipelineToken> captor = ArgumentCaptor.forClass(PipelineToken.class);
        then(pipelineTokenRepository).should().save(captor.capture());
        assertThat(captor.getValue().isRevoked()).isTrue();
    }
}
