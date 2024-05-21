package org.uniprot.api.async.download.refactor.producer.uniprotkb;

import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.uniprot.api.async.download.messaging.config.common.MessageProducerConfig;
import org.uniprot.api.async.download.messaging.config.common.RabbitMQConfigs;
import org.uniprot.api.async.download.messaging.config.common.RedisConfiguration;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBRabbitMQConfig;
import org.uniprot.api.async.download.messaging.listener.common.BaseAbstractMessageListener;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatConfig;
import org.uniprot.api.async.download.messaging.listener.uniprotkb.UniProtKBMessageListener;
import org.uniprot.api.async.download.messaging.repository.UniProtKBDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBDownloadResultWriter;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.consumer.uniprotkb.UniProtKBContentBasedAndRetriableMessageConsumer;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.rest.download.model.JobStatus;

import javax.annotation.PreDestroy;

import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@Import({UniProtKBProducerTestConfig.class, UniProtKBRabbitMQConfig.class, MessageProducerConfig.class, RedisConfiguration.class, RabbitMQConfigs.class})
@EnableConfigurationProperties({HeartbeatConfig.class, UniProtKBDownloadConfigProperties.class})
@TestPropertySource("classpath:application.properties")
class UniProtKBProducerMessageServiceIT {

    private static final String REDIS_IMAGE_VERSION = "redis:6-alpine";
    private static final String RABBITMQ_IMAGE_VERSION = "rabbitmq:3-management";

    @Container
    private static GenericContainer redisServer =
            new GenericContainer(DockerImageName.parse(REDIS_IMAGE_VERSION))
                    .withExposedPorts(6379)
                    .withReuse(true);

    @Container
    protected static RabbitMQContainer rabbitMQContainer =
            new RabbitMQContainer(DockerImageName.parse(RABBITMQ_IMAGE_VERSION));

    @Autowired
    private UniProtKBProducerMessageService service;

    @Autowired
    MessageConverter converter;

    @Autowired
    UniProtKBDownloadJobRepository jobRepository;

    @MockBean
    private UniProtKBContentBasedAndRetriableMessageConsumer uniProtKBConsumer;

    //TODO: uniProtKBListener and uniProtKBWriter need to be remove when we organise HeartBeat bean config
    @MockBean
    private UniProtKBMessageListener uniProtKBListener;
    @MockBean
    private UniProtKBDownloadResultWriter uniProtKBWriter;

    @Captor
    ArgumentCaptor<Message> messageCaptor;

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

    protected static void validateDownloadJob(String jobId, DownloadJob downloadJob, UniProtKBDownloadRequest request) {
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

    protected void validateMessage(Message message, String jobId, UniProtKBDownloadRequest request) {
        assertNotNull(message);
        assertNotNull(message.getMessageProperties());
        MessageProperties messageValues = message.getMessageProperties();
        assertEquals("application/json", messageValues.getContentType());
        assertEquals("UTF-8", messageValues.getContentEncoding());
        assertNotNull(messageValues.getHeaders().get(BaseAbstractMessageListener.JOB_ID_HEADER));

        //Validate Message Header data
        String jobFromHeader = (String) messageValues.getHeaders().get(BaseAbstractMessageListener.JOB_ID_HEADER);
        assertEquals(jobId, jobFromHeader);

        //Validate received UniProtKBDownloadRequest from Message
        UniProtKBDownloadRequest submittedRequest = (UniProtKBDownloadRequest) converter.fromMessage(message);
        assertEquals(request, submittedRequest);
    }

    @Test
    void sendMessage_jobAlreadyExistAndNotAllowed() {

    }

    @Test
    void sendMessage_withoutForceAndAllowed() {

    }

    @Test
    void sendMessage_withForceAndAllowed() {
        UniProtKBDownloadRequest request = new UniProtKBDownloadRequest();
        request.setQuery("query value");
        request.setSort("accession asc");
        request.setFormat("json");
        request.setFields("accession,gene");

        String jobId = service.sendMessage(request);
        assertEquals("60ba2e259320dcb5a23f2e432c8f6bc6d8ed417f", jobId);

        Mockito.verify(uniProtKBConsumer, Mockito.timeout(1000).times(1)).onMessage(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        validateMessage(message, jobId, request);

        //Validate cached data
        DownloadJob downloadJob = jobRepository.findById(jobId)
                .orElseThrow(AssertionFailedError::new);
        validateDownloadJob(jobId, downloadJob, request);
    }

    @Test
    void sendMessage_withForceAndAllowed2() {
        UniProtKBDownloadRequest request = new UniProtKBDownloadRequest();
        request.setQuery("query2 value");
        request.setSort("accession2 asc");
        request.setFormat("json");
        request.setFields("accession,gene");

        String jobId = service.sendMessage(request);

        assertEquals("1a9848044d4910bc55dccdaa673f4713d5ab091e", jobId);
        Mockito.verify(uniProtKBConsumer, Mockito.timeout(1000).times(1)).onMessage(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        validateMessage(message, jobId, request);

        //Validate cached data
        DownloadJob downloadJob = jobRepository.findById(jobId)
                .orElseThrow(AssertionFailedError::new);
        validateDownloadJob(jobId, downloadJob, request);
    }

    @Test
    void sendMessage_withForceAndAllowed3() {
        UniProtKBDownloadRequest request = new UniProtKBDownloadRequest();
        request.setQuery("query3 value");
        request.setSort("accession3 asc");
        request.setFormat("json");
        request.setFields("accession,gene");

        String jobId = service.sendMessage(request);

        assertEquals("975516c6b26115d2d1e0e3a0903feaae618e0bfb", jobId);
        Mockito.verify(uniProtKBConsumer, Mockito.timeout(1000).times(1)).onMessage(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        validateMessage(message, jobId, request);

        //Validate cached data
        DownloadJob downloadJob = jobRepository.findById(jobId)
                .orElseThrow(AssertionFailedError::new);
        validateDownloadJob(jobId, downloadJob, request);
    }

}
