package com.financeiro.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.financeiro.integration.support.PostgresIntegrationTestConfiguration;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Minimal infrastructure integration test for TECH-005.
 *
 * <p>Proves that: a real PostgreSQL 16 container is started via Testcontainers, Spring Boot
 * connects to it through {@code @ServiceConnection}, the {@code ApplicationContext} loads with real
 * {@code DataSourceAutoConfiguration} and {@code FlywayAutoConfiguration} enabled, and Flyway
 * successfully applies the real migration {@code V1__flyway_bootstrap.sql} before JPA initializes
 * an open {@link EntityManagerFactory} with schema validation enabled.
 *
 * <p>It intentionally does not assert anything about business schema, entities, or repositories,
 * and does not assert the total row count of {@code flyway_schema_history}, so it remains valid
 * once future migrations are added.
 */
@SpringBootTest
@ActiveProfiles("it")
@Import(PostgresIntegrationTestConfiguration.class)
class FlywayMigrationIT {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void flywayAppliesBootstrapMigrationBeforeJpaInitializes() {
    Boolean success =
        jdbcTemplate.queryForObject(
            "SELECT success FROM flyway_schema_history WHERE version = ?", Boolean.class, "1");

    assertThat(success).isTrue();
    assertThat(entityManagerFactory.isOpen()).isTrue();
  }
}
