package com.strava.pipeline.token;

import com.strava.pipeline.exception.ApiRequestException;
import com.strava.shared.security.HeaderAuthenticationFilter.HeaderAuthenticationToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PipelineTokenController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PipelineTokenController")
class PipelineTokenControllerTest {

    private static final long USER_ID = 7L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PipelineTokenService pipelineTokenService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new HeaderAuthenticationToken(
                USER_ID, "user@test.dev", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static PipelineToken token(long id, String name) {
        return PipelineToken.builder()
                .id(id)
                .userId(USER_ID)
                .name(name)
                .tokenHash("hash")
                .tokenSuffix("7f2a")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST creates a token and returns the plaintext exactly once")
    void createReturnsPlaintext() throws Exception {
        given(pipelineTokenService.issueToken(USER_ID, "iPhone"))
                .willReturn(new PipelineTokenService.IssuedToken(token(1L, "iPhone"), "tri_plaintext7f2a"));

        mockMvc.perform(post("/api/v1/pipeline/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"iPhone\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("iPhone"))
                .andExpect(jsonPath("$.token").value("tri_plaintext7f2a"))
                .andExpect(jsonPath("$.suffix").value("7f2a"))
                .andExpect(jsonPath("$.revoked").value(false));
    }

    @Test
    @DisplayName("POST without a body issues a default-named token")
    void createWithoutBody() throws Exception {
        given(pipelineTokenService.issueToken(USER_ID, null))
                .willReturn(new PipelineTokenService.IssuedToken(token(2L, "API token"), "tri_plaintext9c1d"));

        mockMvc.perform(post("/api/v1/pipeline/tokens"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("API token"))
                .andExpect(jsonPath("$.token").value("tri_plaintext9c1d"));
    }

    @Test
    @DisplayName("GET lists tokens without ever echoing the plaintext")
    void listOmitsPlaintext() throws Exception {
        given(pipelineTokenService.listTokens(USER_ID)).willReturn(List.of(token(1L, "iPhone")));

        mockMvc.perform(get("/api/v1/pipeline/tokens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].suffix").value("7f2a"))
                .andExpect(jsonPath("$[0].token").doesNotExist());
    }

    @Test
    @DisplayName("DELETE revokes and returns the contract's message body")
    void revoke() throws Exception {
        mockMvc.perform(delete("/api/v1/pipeline/tokens/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token revoked"));
    }

    @Test
    @DisplayName("DELETE of a foreign token surfaces the ProblemDetail 404 with message")
    void revokeNotFound() throws Exception {
        willThrow(new ApiRequestException("Token not found", HttpStatus.NOT_FOUND))
                .given(pipelineTokenService).revokeToken(USER_ID, 9L);

        mockMvc.perform(delete("/api/v1/pipeline/tokens/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Token not found"));
    }

    @Test
    @DisplayName("create without a name field delegates a null name")
    void createWithEmptyJson() throws Exception {
        given(pipelineTokenService.issueToken(USER_ID, null))
                .willReturn(new PipelineTokenService.IssuedToken(token(3L, "API token"), "tri_plaintext1a2b"));

        mockMvc.perform(post("/api/v1/pipeline/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("API token"));
    }
}
