package org.uniprot.api.async.download.messaging.producer;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.PreDestroy;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.common.MessageProducerConfig;
import org.uniprot.api.async.download.messaging.config.common.RabbitMQConfigs;
import org.uniprot.api.async.download.messaging.config.common.RedisConfiguration;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatConfig;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(
        classes = {MessageProducerConfig.class, RedisConfiguration.class, RabbitMQConfigs.class})
@EnableConfigurationProperties({HeartbeatConfig.class})
@TestPropertySource("classpath:application.properties")
public abstract class BasicProducerMessageServiceIT {

    private static final String REDIS_IMAGE_VERSION = "redis:6-alpine";
    private static final String RABBITMQ_IMAGE_VERSION = "rabbitmq:3-management";

    @Autowired protected MessageConverter converter;

    @TempDir private Path tempDir;

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
    }

    protected static void validateDownloadJob(String jobId, DownloadJob downloadJob) {
        assertEquals(jobId, downloadJob.getId());
        assertEquals(JobStatus.NEW, downloadJob.getStatus());
        assertNotNull(downloadJob.getCreated());
        assertNotNull(downloadJob.getUpdated());
        assertNull(downloadJob.getError());
        assertNull(downloadJob.getResultFile());
        assertEquals(0, downloadJob.getRetried());
        assertEquals(0, downloadJob.getTotalEntries());
        assertEquals(0, downloadJob.getProcessedEntries());
        assertEquals(0, downloadJob.getUpdateCount());
    }

    protected static void validateMessage(Message message, String jobId) {
        assertNotNull(message);
        assertNotNull(message.getMessageProperties());
        MessageProperties messageValues = message.getMessageProperties();
        assertEquals("application/json", messageValues.getContentType());
        assertEquals("UTF-8", messageValues.getContentEncoding());
        assertNotNull(messageValues.getHeaders().get("jobId"));

        // Validate Message Header data
        String jobFromHeader = (String) messageValues.getHeaders().get("jobId");
        assertEquals(jobId, jobFromHeader);
    }

    protected void createJobFiles(String jobId) throws IOException {
        getDownloadConfigProperties()
                .setIdFilesFolder(
                        tempDir
                                + File.separator
                                + getDownloadConfigProperties().getIdFilesFolder());
        getDownloadConfigProperties()
                .setResultFilesFolder(
                        tempDir
                                + File.separator
                                + getDownloadConfigProperties().getResultFilesFolder());
        Files.createDirectories(Path.of(getDownloadConfigProperties().getIdFilesFolder()));
        Files.createDirectories(Path.of(getDownloadConfigProperties().getResultFilesFolder()));
        assertTrue(getFileHandler().getIdFile(jobId).toFile().createNewFile());
        assertTrue(getFileHandler().getResultFile(jobId).toFile().createNewFile());
        assertTrue(getFileHandler().isIdFileExist(jobId));
        assertTrue(getFileHandler().isResultFileExist(jobId));
    }

    protected abstract DownloadConfigProperties getDownloadConfigProperties();

    protected abstract AsyncDownloadFileHandler getFileHandler();
}
