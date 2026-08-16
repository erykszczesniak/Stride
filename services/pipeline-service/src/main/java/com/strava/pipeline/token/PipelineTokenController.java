package com.strava.pipeline.token;

import com.strava.pipeline.token.PipelineTokenDtos.CreateTokenRequest;
import com.strava.pipeline.token.PipelineTokenDtos.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Management endpoints for pipeline personal API tokens, ported 1:1 from
 * main. Lives under /api/v1 (gateway-validated JWT, identity via X-User-*
 * headers): a logged-in user issues a token here, then uses it as the Bearer
 * credential against /pipeline/v1/**.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pipeline/tokens")
public class PipelineTokenController {

    private final PipelineTokenService pipelineTokenService;

    @PostMapping
    public ResponseEntity<TokenResponse> createToken(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody(required = false) CreateTokenRequest request) {
        PipelineTokenService.IssuedToken issued = pipelineTokenService.issueToken(
                userId,
                request == null ? null : request.name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(issued.token(), issued.plaintext()));
    }

    @GetMapping
    public ResponseEntity<List<TokenResponse>> listTokens(@AuthenticationPrincipal Long userId) {
        List<TokenResponse> tokens = pipelineTokenService.listTokens(userId).stream()
                .map(token -> toResponse(token, null))
                .toList();
        return ResponseEntity.ok(tokens);
    }

    @DeleteMapping("/{tokenId}")
    public ResponseEntity<Map<String, String>> revokeToken(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long tokenId) {
        pipelineTokenService.revokeToken(userId, tokenId);
        return ResponseEntity.ok(Map.of("message", "Token revoked"));
    }

    private TokenResponse toResponse(PipelineToken token, String plaintext) {
        return new TokenResponse(
                token.getId(),
                token.getName(),
                plaintext,
                token.getTokenSuffix(),
                token.getCreatedAt(),
                token.getLastUsedAt(),
                token.isRevoked());
    }
}
