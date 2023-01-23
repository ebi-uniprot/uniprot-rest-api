package org.uniprot.api.uniprotkb.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class ExampleRedisTestContainerTest {
    @Container
    public GenericContainer redis =
            new GenericContainer(DockerImageName.parse("redis:5.0.3-alpine"))
                    .withExposedPorts(6379)
                    .withReuse(true);

    @BeforeEach
    public void setUp() {
        String address = redis.getHost();
        Integer port = redis.getFirstMappedPort();
    }

    @Test
    public void testSimplePutAndGet() {
        Assertions.assertTrue(this.redis.isRunning());
    }
}
