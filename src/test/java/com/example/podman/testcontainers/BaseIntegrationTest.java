package com.example.podman.testcontainers;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
public abstract class BaseIntegrationTest {

  public static final int REDIS_PORT = 6379;

  private static final PostgreSQLContainer<?> POSTGRES;
  private static final GenericContainer<?> REDIS;

  static {
    POSTGRES = createPostgresContainer();
    REDIS = createRedisContainer();
  }

  private static PostgreSQLContainer<?> createPostgresContainer() {
    PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:14-alpine"));
    postgres.start();
    Runtime.getRuntime().addShutdownHook(new Thread(postgres::stop));
    return postgres;
  }

  private static GenericContainer<?> createRedisContainer() {
    GenericContainer<?> redis =
        new GenericContainer<>(DockerImageName.parse("redis:6-alpine"))
            .withExposedPorts(REDIS_PORT);
    redis.start();
    Runtime.getRuntime().addShutdownHook(new Thread(redis::stop));
    return redis;
  }

  @DynamicPropertySource
  static void registerPostgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.r2dbc.url", BaseIntegrationTest::getPostgresR2bcUrl);
    registry.add("spring.r2dbc.username", POSTGRES::getUsername);
    registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
    registry.add("spring.flyway.user", POSTGRES::getUsername);
    registry.add("spring.flyway.password", POSTGRES::getPassword);
  }

  @DynamicPropertySource
  static void registerRedisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.redis.host", REDIS::getContainerIpAddress);
    registry.add("spring.redis.port", () -> REDIS.getMappedPort(REDIS_PORT));
  }

  private static String getPostgresR2bcUrl() {
    return "r2dbc:postgresql://"
        + POSTGRES.getContainerIpAddress()
        + ":"
        + POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)
        + "/"
        + POSTGRES.getDatabaseName();
  }
}
