package org.uniprot.api.async.download.messaging.consumer;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.uniprot.api.async.download.common.AbstractDownloadIT;
import org.uniprot.api.async.download.messaging.config.common.MessageProducerConfig;
import org.uniprot.api.async.download.messaging.config.common.RabbitMQConfigs;
import org.uniprot.api.async.download.messaging.config.common.RedisConfiguration;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatConfig;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({MessageProducerConfig.class, RedisConfiguration.class, RabbitMQConfigs.class})
@EnableConfigurationProperties({HeartbeatConfig.class})
@TestPropertySource("classpath:application.properties")
public abstract class BasicMessageConsumerIT extends AbstractDownloadIT {
    /*
    private static final String REDIS_IMAGE_VERSION = "redis:6-alpine";
    private static final String RABBITMQ_IMAGE_VERSION = "rabbitmq:3-management";

    @TempDir
    private Path tempDir;

    @Container
    private static GenericContainer redisServer =
            new GenericContainer(DockerImageName.parse(REDIS_IMAGE_VERSION))
                    .withExposedPorts(6379)
                    .withReuse(true);

    @Container
    protected static RabbitMQContainer rabbitMQContainer =
            new RabbitMQContainer(DockerImageName.parse(RABBITMQ_IMAGE_VERSION));

    @PreDestroy
    public void preDestroy() {
        redisServer.stop();
        rabbitMQContainer.stop();
    }

    @DynamicPropertySource
    public static void setUpThings(DynamicPropertyRegistry propertyRegistry) {
        Startables.deepStart(rabbitMQContainer, redisServer).join();
        assertTrue(rabbitMQContainer.isRunning());
        assertTrue(redisServer.isRunning());
        propertyRegistry.add("spring.amqp.rabbit.port", rabbitMQContainer::getFirstMappedPort);
        propertyRegistry.add("spring.amqp.rabbit.host", rabbitMQContainer::getHost);
        System.setProperty("uniprot.redis.host", redisServer.getHost());
        System.setProperty("uniprot.redis.port", String.valueOf(redisServer.getFirstMappedPort()));
        propertyRegistry.add("ALLOW_EMPTY_PASSWORD", () -> "yes");
    }*/
}
