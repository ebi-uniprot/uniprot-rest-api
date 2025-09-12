package org.uniprot.api.idmapping.common.service;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class RedisTestContainer {
    private static final GenericContainer<?> REDIS_CONTAINER;

    static {
        REDIS_CONTAINER =
                new GenericContainer<>(DockerImageName.parse("redis:6-alpine"))
                        .withExposedPorts(6379)
                        .withReuse(true)
                        .withCommand("redis-server --save \"\" --appendonly no");
        REDIS_CONTAINER.start();
    }

    public static GenericContainer<?> getInstance() {
        return REDIS_CONTAINER;
    }

    private RedisTestContainer() {}
}
