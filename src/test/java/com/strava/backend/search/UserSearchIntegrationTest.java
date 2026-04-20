package com.strava.backend.search;

import com.strava.backend.domain.User;
import com.strava.backend.enums.AuthProvider;
import com.strava.backend.enums.Role;
import com.strava.backend.event.UserUpsertedEvent;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Single end-to-end Elasticsearch integration test: spins up a real ES 8.11.4
 * container, publishes a {@link UserUpsertedEvent}, and asserts the user becomes
 * searchable by bio text.
 *
 * <p>ES integration tests are expensive (container startup dominates), so we
 * intentionally keep exactly one.
 *
 * <p>The test is gated on Docker availability: if the worktree does not expose
 * a Docker daemon (some CI / sandboxed environments), the test class auto-skips
 * via {@link EnabledIf}. If Testcontainers cannot even locate Docker, the class
 * falls back to {@link Disabled}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
@EnabledIf("isDockerAvailable")
@DisplayName("User search — Elasticsearch integration")
class UserSearchIntegrationTest {

    static boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    static final ElasticsearchContainer ES = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.4"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @BeforeAll
    static void startContainer() {
        ES.start();
    }

    @AfterAll
    static void stopContainer() {
        ES.stop();
    }

    @DynamicPropertySource
    static void registerEsProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", () -> "http://" + ES.getHttpHostAddress());
    }

    @Autowired
    private UserSearchRepository userSearchRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void wipeIndex() {
        userSearchRepository.deleteAll();
    }

    @Test
    @DisplayName("publishing UserUpsertedEvent makes the user searchable by bio text")
    void savingAUserMakesThemSearchableByBio() {
        User ada = User.builder()
                .id(1001L)
                .firstName("Ada")
                .lastName("Lovelace")
                .email("ada@example.com")
                .bio("passionate marathon runner and cyclist")
                .city("London")
                .country("UK")
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .hideFromSearch(false)
                .build();

        eventPublisher.publishEvent(new UserUpsertedEvent(ada));

        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Page<UserDocument> hits = userSearchRepository.searchByQuery(
                            "marathon", PageRequest.of(0, 10));
                    assertThat(hits.getContent())
                            .extracting(UserDocument::getId)
                            .contains(1001L);
                });
    }
}
