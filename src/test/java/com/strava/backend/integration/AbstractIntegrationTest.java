package com.strava.backend.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Abstract base class for integration tests using Testcontainers with PostgreSQL.
 *
 * <p>This provides a shared PostgreSQL container that is reused across all test classes
 * that extend this base class. The @ServiceConnection annotation automatically configures
 * Spring Boot's DataSource to connect to the Testcontainers PostgreSQL instance.
 *
 * <p>Modern Java 21 / Spring Boot 3.4 features used:
 * <ul>
 *   <li>@ServiceConnection - automatic DataSource configuration (Spring Boot 3.1+)</li>
 *   <li>@Testcontainers - JUnit 5 extension for container lifecycle</li>
 *   <li>Static container for reuse across tests</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * class MyIntegrationTest extends AbstractIntegrationTest {
 *     @Autowired
 *     private MyRepository myRepository;
 *
 *     @Test
 *     void shouldDoSomething() {
 *         // Test using real PostgreSQL
 *     }
 * }
 * }</pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("testcontainers")
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("strava_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);
}
