package com.strava.backend.controller;

import com.strava.backend.domain.PipelineToken;
import com.strava.backend.dto.pipeline.PipelineTokenDtos.CreateTokenRequest;
import com.strava.backend.dto.pipeline.PipelineTokenDtos.TokenResponse;
import com.strava.backend.service.pipeline.PipelineTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static com.strava.backend.constants.PathConstants.API_V1_PIPELINE_TOKENS;

/**
 * Management endpoints for pipeline personal API tokens. Lives under /api/v1
 * (JWT-authenticated): a logged-in user issues a token here, then uses it as
 * the Bearer credential against /pipeline/v1/**.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_PIPELINE_TOKENS)
public class PipelineTokenController extends BaseController {

    private final PipelineTokenService pipelineTokenService;

    @PostMapping
    public ResponseEntity<TokenResponse> createToken(@Valid @RequestBody(required = false) CreateTokenRequest request) {
        PipelineTokenService.IssuedToken issued = pipelineTokenService.issueToken(
                getAuthenticatedUserId(),
                request == null ? null : request.name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(issued.token(), issued.plaintext()));
    }

    @GetMapping
    public ResponseEntity<List<TokenResponse>> listTokens() {
        List<TokenResponse> tokens = pipelineTokenService.listTokens(getAuthenticatedUserId()).stream()
                .map(token -> toResponse(token, null))
                .toList();
        return ResponseEntity.ok(tokens);
    }

    @DeleteMapping("/{tokenId}")
    public ResponseEntity<Map<String, String>> revokeToken(@PathVariable Long tokenId) {
        pipelineTokenService.revokeToken(getAuthenticatedUserId(), tokenId);
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
