package com.financeiro.integration.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Reusable test configuration exposing a real PostgreSQL 16 Testcontainers instance as a
 * Spring-managed bean.
 *
 * <p>The container is annotated with {@link ServiceConnection}, so Spring Boot automatically wires
 * the datasource against it (host, port, database, credentials) without any manual
 * {@code @DynamicPropertySource} configuration.
 *
 * <p>Integration tests import this configuration explicitly via {@code @Import}, favoring
 * composition over a generic abstract base class so future repository integration tests can reuse
 * it without being forced into an inheritance hierarchy.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresIntegrationTestConfiguration {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16");

  @Bean
  @ServiceConnection
  PostgreSQLContainer<?> postgresContainer() {
    return new PostgreSQLContainer<>(POSTGRES_IMAGE);
  }
}
