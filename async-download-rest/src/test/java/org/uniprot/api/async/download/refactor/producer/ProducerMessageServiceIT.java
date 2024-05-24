package org.uniprot.api.async.download.refactor.producer;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
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
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatConfig;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.request.SolrStreamDownloadRequest;
import org.uniprot.api.rest.download.model.JobStatus;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.rest.output.UniProtMediaType.valueOf;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({MessageProducerConfig.class, RedisConfiguration.class, RabbitMQConfigs.class})
@EnableConfigurationProperties({HeartbeatConfig.class})
@TestPropertySource("classpath:application.properties")
public abstract class ProducerMessageServiceIT {

    private static final String REDIS_IMAGE_VERSION = "redis:6-alpine";
    private static final String RABBITMQ_IMAGE_VERSION = "rabbitmq:3-management";

    @Autowired
    MessageConverter converter;

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
        System.setProperty(
                "uniprot.redis.port", String.valueOf(redisServer.getFirstMappedPort()));
        propertyRegistry.add("ALLOW_EMPTY_PASSWORD", () -> "yes");
    }

    protected static void validateDownloadJob(String jobId, DownloadJob downloadJob, SolrStreamDownloadRequest request) {
        assertEquals(jobId, downloadJob.getId());
        assertEquals(JobStatus.NEW, downloadJob.getStatus());
        assertNotNull(downloadJob.getCreated());
        assertNotNull(downloadJob.getUpdated());
        assertNull(downloadJob.getError());
        assertEquals(0, downloadJob.getRetried());
        assertEquals(request.getQuery(), downloadJob.getQuery());
        assertEquals(request.getFields(), downloadJob.getFields());
        assertEquals(request.getSort(), downloadJob.getSort());
        assertNull(downloadJob.getResultFile());
        assertEquals(valueOf(request.getFormat()), valueOf(downloadJob.getFormat()));
        assertEquals(0, downloadJob.getTotalEntries());
        assertEquals(0, downloadJob.getProcessedEntries());
        assertEquals(0, downloadJob.getUpdateCount());
    }

    protected void validateMessage(Message message, String jobId, SolrStreamDownloadRequest request) {
        assertNotNull(message);
        assertNotNull(message.getMessageProperties());
        MessageProperties messageValues = message.getMessageProperties();
        assertEquals("application/json", messageValues.getContentType());
        assertEquals("UTF-8", messageValues.getContentEncoding());
        assertNotNull(messageValues.getHeaders().get("jobId"));

        //Validate Message Header data
        String jobFromHeader = (String) messageValues.getHeaders().get("jobId");
        assertEquals(jobId, jobFromHeader);

        //Validate received UniProtKBDownloadRequest from Message
        SolrStreamDownloadRequest submittedRequest = (SolrStreamDownloadRequest) converter.fromMessage(message);
        assertEquals(request, submittedRequest);
    }

    protected void createJobFiles(String jobId, AsyncDownloadFileHandler fileHandler, DownloadConfigProperties downloadConfigProperties) throws IOException {
        downloadConfigProperties.setIdFilesFolder(tempDir + File.separator + downloadConfigProperties.getIdFilesFolder());
        downloadConfigProperties.setResultFilesFolder(tempDir + File.separator +  downloadConfigProperties.getResultFilesFolder());
        Files.createDirectories(Path.of(downloadConfigProperties.getIdFilesFolder()));
        Files.createDirectories(Path.of(downloadConfigProperties.getResultFilesFolder()));
        assertTrue(fileHandler.getIdFile(jobId).toFile().createNewFile());
        assertTrue(fileHandler.getResultFile(jobId).toFile().createNewFile());
    }

}
