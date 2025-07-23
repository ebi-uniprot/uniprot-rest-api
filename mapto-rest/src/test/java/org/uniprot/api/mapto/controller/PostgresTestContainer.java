package org.uniprot.api.mapto.controller;

import java.time.Duration;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class PostgresTestContainer {
    private static final String POSTGRES_IMAGE_VERSION = "postgres:11.1";
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

    static {
        POSTGRES_CONTAINER =
                new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE_VERSION))
                        .withStartupTimeout(Duration.ofMinutes(2))
                        .withInitScript("init.sql");
        POSTGRES_CONTAINER.start();
    }

    public static PostgreSQLContainer<?> getInstance() {
        return POSTGRES_CONTAINER;
    }

    private PostgresTestContainer() {}
}
