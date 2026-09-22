package com.devjubis.tickets.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class PostgresContainerTest {

    private static final PostgreSQLContainer<?> POSTGRES = startContainerWhenDockerIsReachable();

    private static PostgreSQLContainer<?> startContainerWhenDockerIsReachable() {
        if (!DockerClientFactory.instance().isDockerAvailable()) {
            return null;
        }
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:16-alpine");
        container.start();
        return container;
    }

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        if (POSTGRES == null) {
            return;
        }
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
