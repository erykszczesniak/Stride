package com.strava.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.domain.User;
import com.strava.backend.dto.user.UpdateUserRequest;
import com.strava.backend.enums.AuthProvider;
import com.strava.backend.enums.Role;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.security.UserPrincipal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.DockerClientFactory;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for User API using Testcontainers with PostgreSQL.
 *
 * <p>These tests verify the full request/response cycle with a real database,
 * testing the integration between controllers, services, and repositories.
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("User API Integration Tests (Testcontainers)")
@EnabledIf("isDockerAvailable")
class UserIntegrationTest extends AbstractIntegrationTest {

    static boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User testUser;
    private User anotherUser;
    private UsernamePasswordAuthenticationToken testAuth;
    private UsernamePasswordAuthenticationToken anotherAuth;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = User.builder()
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .password("encodedPassword123")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .active(true)
                .bio("Test bio")
                .city("Warsaw")
                .country("Poland")
                .build();
        testUser = userRepository.save(testUser);
        UserPrincipal userPrincipal = UserPrincipal.create(testUser);
        testAuth = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());

        anotherUser = User.builder()
                .email("jane@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .password("encodedPassword456")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .active(true)
                .bio("Another bio")
                .city("Krakow")
                .country("Poland")
                .build();
        anotherUser = userRepository.save(anotherUser);
        UserPrincipal anotherPrincipal = UserPrincipal.create(anotherUser);
        anotherAuth = new UsernamePasswordAuthenticationToken(anotherPrincipal, null, anotherPrincipal.getAuthorities());
    }

    @Nested
    @DisplayName("GET /api/v1/users/{userId}")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return user when found")
        void shouldReturnUserWhenFound() throws Exception {
            mockMvc.perform(get("/api/v1/users/{userId}", testUser.getId())
                            .with(authentication(testAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testUser.getId()))
                    .andExpect(jsonPath("$.email").value("john@example.com"))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.city").value("Warsaw"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent user")
        void shouldReturn404ForNonExistentUser() throws Exception {
            mockMvc.perform(get("/api/v1/users/{userId}", 99999L)
                            .with(authentication(testAuth)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should return current user profile")
        void shouldReturnCurrentUserProfile() throws Exception {
            mockMvc.perform(get("/api/v1/users/me")
                            .with(authentication(testAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testUser.getId()))
                    .andExpect(jsonPath("$.email").value("john@example.com"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return paginated list of users")
        void shouldReturnPaginatedUsers() throws Exception {
            mockMvc.perform(get("/api/v1/users")
                            .with(authentication(testAuth))
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user profile")
        void shouldUpdateUserProfile() throws Exception {
            var updateRequest = new UpdateUserRequest();
            updateRequest.setFirstName("Johnny");
            updateRequest.setLastName("Updated");
            updateRequest.setBio("Updated bio");
            updateRequest.setCity("Gdansk");
            updateRequest.setCountry("Poland");

            mockMvc.perform(put("/api/v1/users/me")
                            .with(authentication(testAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Johnny"))
                    .andExpect(jsonPath("$.lastName").value("Updated"))
                    .andExpect(jsonPath("$.city").value("Gdansk"));

            // Verify in database
            User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
            assertThat(updatedUser.getFirstName()).isEqualTo("Johnny");
            assertThat(updatedUser.getCity()).isEqualTo("Gdansk");
        }
    }

    @Nested
    @DisplayName("Follow/Unfollow User Tests")
    class FollowUserTests {

        @Test
        @DisplayName("Should follow and unfollow user")
        void shouldFollowAndUnfollowUser() throws Exception {
            // Follow user
            mockMvc.perform(post("/api/v1/users/{userId}/follow", anotherUser.getId())
                            .with(authentication(testAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User followed"));

            // Verify follow relationship in database
            entityManager.flush();
            entityManager.clear();
            User follower = userRepository.findById(testUser.getId()).orElseThrow();
            User followed = userRepository.findById(anotherUser.getId()).orElseThrow();
            assertThat(follower.getFollowing()).contains(followed);

            // Unfollow user
            mockMvc.perform(delete("/api/v1/users/{userId}/follow", anotherUser.getId())
                            .with(authentication(testAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User unfollowed"));

            // Verify unfollow in database
            entityManager.flush();
            entityManager.clear();
            follower = userRepository.findById(testUser.getId()).orElseThrow();
            followed = userRepository.findById(anotherUser.getId()).orElseThrow();
            assertThat(follower.getFollowing()).doesNotContain(followed);
        }

        @Test
        @DisplayName("Should return error when following self")
        void shouldReturnErrorWhenFollowingSelf() throws Exception {
            mockMvc.perform(post("/api/v1/users/{userId}/follow", testUser.getId())
                            .with(authentication(testAuth)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{userId}/followers")
    class GetFollowersTests {

        @Test
        @DisplayName("Should return followers list")
        void shouldReturnFollowersList() throws Exception {
            // First, make testUser follow anotherUser
            mockMvc.perform(post("/api/v1/users/{userId}/follow", anotherUser.getId())
                            .with(authentication(testAuth)))
                    .andExpect(status().isOk());

            // Get followers of anotherUser
            mockMvc.perform(get("/api/v1/users/{userId}/followers", anotherUser.getId())
                            .with(authentication(testAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].firstName").value("John"));
        }

        @Test
        @DisplayName("Should return empty list when no followers")
        void shouldReturnEmptyListWhenNoFollowers() throws Exception {
            mockMvc.perform(get("/api/v1/users/{userId}/followers", testUser.getId())
                            .with(authentication(testAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{userId}/following")
    class GetFollowingTests {

        @Test
        @DisplayName("Should return following list")
        void shouldReturnFollowingList() throws Exception {
            // First, make testUser follow anotherUser
            mockMvc.perform(post("/api/v1/users/{userId}/follow", anotherUser.getId())
                            .with(authentication(testAuth)))
                    .andExpect(status().isOk());

            // Clear Hibernate session to force fresh load
            entityManager.flush();
            entityManager.clear();

            // Get following list of testUser
            mockMvc.perform(get("/api/v1/users/{userId}/following", testUser.getId())
                            .with(authentication(testAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].firstName").value("Jane"));
        }
    }
}
