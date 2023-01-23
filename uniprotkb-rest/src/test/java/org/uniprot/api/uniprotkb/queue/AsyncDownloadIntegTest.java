package org.uniprot.api.uniprotkb.queue;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.uniprot.api.uniprotkb.controller.UniProtKBDownloadController.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.rest.download.MessageQueueTestConfig;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.DownloadRequestToArrayConverter;
import org.uniprot.api.rest.download.model.HashGenerator;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.ProducerMessageService;
import org.uniprot.api.rest.download.repository.CommonRestTestConfig;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.controller.AbstractUniProtKBDownloadIT;
import org.uniprot.api.uniprotkb.controller.UniProtKBDownloadController;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBStreamRequest;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.api.uniprotkb.repository.store.UniProtStoreConfig;
import org.uniprot.store.search.SolrCollection;

import com.jayway.jsonpath.JsonPath;

@ActiveProfiles(profiles = "offline")
@EnableConfigurationProperties
@PropertySource("classpath:application.properties")
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            UniProtKBREST.class,
            UniProtStoreConfig.class,
            MessageQueueTestConfig.class,
            CommonRestTestConfig.class
        })
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebMvcTest({UniProtKBDownloadController.class})
@AutoConfigureWebClient
public class AsyncDownloadIntegTest extends AbstractUniProtKBDownloadIT {
    @Autowired private TupleStreamTemplate tupleStreamTemplate;
    @Autowired private FacetTupleStreamTemplate facetTupleStreamTemplate;

    @Autowired
    @Qualifier("uniProtKBSolrClient")
    private SolrClient
            solrClient; // this is NOT inmemory solr cluster client, see CloudSolrClient in parent
    // class

    @Autowired private RabbitTemplate rabbitTemplate; // RabbitTemplate with inmemory qpid broker

    @Autowired private AmqpAdmin amqpAdmin;

    @SpyBean @Autowired
    private DownloadJobRepository
            downloadJobRepository; // RedisRepository with inMemory redis server

    @SpyBean @Autowired private UniProtKBMessageListener uniProtKBMessageListener;
    @SpyBean @Autowired private ProducerMessageService messageService;

    @SpyBean @Autowired private MessageConverter messageConverter;

    private HashGenerator<StreamRequest> hashGenerator;

    @Value("${spring.amqp.rabbit.queueName}")
    private String downloadQueue;

    @Value("${spring.amqp.rabbit.retryQueueName}")
    private String retryQueue;

    @Value(("${spring.amqp.rabbit.rejectedQueueName}"))
    private String rejectedQueue;

    @BeforeAll
    void init() {
        this.hashGenerator = new HashGenerator<>(new DownloadRequestToArrayConverter(), SALT_STR);
        this.amqpAdmin.purgeQueue(downloadQueue, true);
    }

    @AfterAll
    void destroy() {
        this.amqpAdmin.purgeQueue(downloadQueue, true);
    }

    @Test
    void sendAndProcessDownloadMessageSuccessfully() throws IOException {
        String query = "*:*";
        String jobId = sendMessage(query);
        // Producer
        verify(this.messageService, never()).logAlreadyProcessed(jobId);
        // redis entry created
        await().until(jobCreatedInRedis(jobId));
        verifyRedisEntry(query, jobId, List.of(JobStatus.NEW, JobStatus.RUNNING), 0, false);
        // rabbitmq broker
        await().until(getMessageCountInQueue(downloadQueue), equalTo(1));
        verifyMessageListener(1, 0);
        verifyRedisEntry(query, jobId, List.of(JobStatus.FINISHED), 0, false);
        verifyIdsAndResultFiles(jobId);
    }

//    @Test FIXME
    void sendAndProcessMessageSuccessfullyAfterRetry() throws IOException {
        doThrow(new RuntimeException("Forced exception for testing on call converter.fromMessage"))
                .doCallRealMethod()
                .when(this.messageConverter)
                .fromMessage(any());
        String query = "*";
        String jobId = sendMessage(query);
        // Producer
        verify(this.messageService, never()).logAlreadyProcessed(jobId);
        await().until(jobCreatedInRedis(jobId));
        // verify  that retry queue count is 1
        await().until(getMessageCountInQueue(retryQueue), equalTo(1));
        await().until(getMessageCountInQueue(downloadQueue), equalTo(0));
        // verify  redis
        verifyRedisEntry(query, jobId, List.of(JobStatus.ERROR), 1, true);
        // after certain delay the retryQueue put the message back to downloadQueue
        await().until(getMessageCountInQueue(downloadQueue), equalTo(1));
        await().until(getMessageCountInQueue(retryQueue), equalTo(0));
        verifyMessageListener(2, 1);
        verifyRedisEntry(query, jobId, List.of(JobStatus.FINISHED), 1, true);
        verifyIdsAndResultFiles(jobId);
    }

    private String sendMessage(String query) {
        UniProtKBStreamRequest request = new UniProtKBStreamRequest();
        request.setQuery(query);
        MessageProperties messageHeader = new MessageProperties();
        String jobId = this.hashGenerator.generateHash(request);
        messageHeader.setHeader(JOB_ID, jobId);
        String contentType = "application/json";
        messageHeader.setHeader(CONTENT_TYPE, contentType);
        this.messageService.sendMessage(request, messageHeader);
        return jobId;
    }

    private void verifyRedisEntry(
            String query, String jobId, List<JobStatus> statuses, int retryCount, boolean isError) {
        Optional<DownloadJob> optDownloadJob = this.downloadJobRepository.findById(jobId);
        assertTrue(optDownloadJob.isPresent());
        System.out.println(optDownloadJob.get());
        assertEquals(jobId, optDownloadJob.get().getId());
        assertEquals(query, optDownloadJob.get().getQuery());
        assertAll(
                () -> assertNull(optDownloadJob.get().getSort()),
                () -> assertNull(optDownloadJob.get().getFields()));
        assertEquals(retryCount, optDownloadJob.get().getRetried());
        assertTrue(statuses.contains(optDownloadJob.get().getStatus()));
        assertEquals(isError, Objects.nonNull(optDownloadJob.get().getError()));
        assertAll(
                () -> assertNotNull(optDownloadJob.get().getCreated()),
                () -> assertNotNull(optDownloadJob.get().getUpdated()));
    }

    private void verifyMessageListener(int timesOnMessage, int timesAddHeader) {
        await().until(getMessageCountInQueue(downloadQueue), equalTo(0));
        verify(this.uniProtKBMessageListener, atLeast(timesOnMessage)).onMessage(any());
        verify(this.uniProtKBMessageListener, atLeast(timesAddHeader))
                .addAdditionalHeaders(any(), any());
        verify(this.uniProtKBMessageListener, times(1)).streamIds(any());
        verify(this.uniProtKBMessageListener, never()).setMaxRetryCount(any());
    }

    private Callable<Boolean> jobCreatedInRedis(String jobId) {
        return () -> this.downloadJobRepository.existsById(jobId);
    }

    private Callable<Integer> getMessageCountInQueue(String queueName) {
        return () -> (Integer) amqpAdmin.getQueueProperties(queueName).get("QUEUE_MESSAGE_COUNT");
    }

    private void verifyIdsAndResultFiles(String jobId) throws IOException {
        // verify the ids file
        Path idsFilePath = Path.of(this.idsFolder + "/" + jobId);
        Assertions.assertTrue(Files.exists(idsFilePath));
        List<String> ids = Files.readAllLines(idsFilePath);
        Assertions.assertNotNull(ids);
        // verify result file
        Path resultFilePath = Path.of(this.resultFolder + "/" + jobId);
        Assertions.assertTrue(Files.exists(resultFilePath));
        String resultsJson = Files.readString(resultFilePath);
        Assertions.assertNotNull(resultsJson);
        List<String> primaryAccessions = JsonPath.read(resultsJson, "$.results.*.primaryAccession");
        Assertions.assertNotNull(primaryAccessions);
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniprot, SolrCollection.taxonomy);
    }

    @Override
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return this.tupleStreamTemplate;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return this.facetTupleStreamTemplate;
    }
}
