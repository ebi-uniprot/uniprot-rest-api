package org.uniprot.api.uniprotkb.queue;

import static com.carrotsearch.ant.tasks.junit4.dependencies.com.google.common.base.Predicates.equalTo;
import static org.awaitility.Awaitility.await;
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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.rest.download.AsyncDownloadTestConfig;
import org.uniprot.api.rest.download.configuration.RedisConfiguration;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.DownloadRequestToArrayConverter;
import org.uniprot.api.rest.download.model.HashGenerator;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.ProducerMessageService;
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

@ActiveProfiles(profiles = {"offline", "asyncDownload"})
@EnableConfigurationProperties
@PropertySource("classpath:application.properties")
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            UniProtKBREST.class,
            UniProtStoreConfig.class,
            AsyncDownloadTestConfig.class,
            RedisConfiguration.class
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

    @SpyBean @Autowired
    private DownloadJobRepository
            downloadJobRepository; // RedisRepository with inMemory redis server

    @SpyBean @Autowired private UniProtKBMessageListener uniProtKBMessageListener;
    @SpyBean @Autowired private ProducerMessageService messageService;

    @SpyBean @Autowired private MessageConverter messageConverter;

    private HashGenerator<StreamRequest> hashGenerator;

    @Value("${spring.amqp.rabbit.retryMaxCount}")
    private int maxRetry;

    @BeforeAll
    void init() {
        this.hashGenerator = new HashGenerator<>(new DownloadRequestToArrayConverter(), SALT_STR);
    }

    @Test
    void sendAndProcessDownloadMessageSuccessfully() throws IOException {
        String query = "*:*";
        UniProtKBStreamRequest request = createStreamRequest(query);
        String jobId = this.hashGenerator.generateHash(request);
        sendMessage(request, jobId);
        // Producer
        verify(this.messageService, never()).logAlreadyProcessed(jobId);
        // redis entry created
        await().until(jobCreatedInRedis(jobId));
        await().until(jobFinished(jobId));
        verifyMessageListener(1, 0, 1);
        verifyRedisEntry(query, jobId, List.of(JobStatus.FINISHED), 0, false);
        verifyIdsAndResultFiles(jobId);
    }

    @Test
    void sendAndProcessMessageSuccessfullyAfterRetry() throws IOException {
        doThrow(new RuntimeException("Forced exception for testing on call converter.fromMessage"))
                .doCallRealMethod()
                .when(this.messageConverter)
                .fromMessage(any());
        String query = "*";
        UniProtKBStreamRequest request = createStreamRequest(query);
        String jobId = this.hashGenerator.generateHash(request);
        sendMessage(request, jobId);
        // Producer
        verify(this.messageService, never()).logAlreadyProcessed(jobId);
        await().until(jobCreatedInRedis(jobId));
        await().until(jobErrored(jobId));
        // verify  redis
        verifyRedisEntry(query, jobId, List.of(JobStatus.ERROR), 1, true);
        // after certain delay the job should be reprocessed
        await().until(jobFinished(jobId));
        verifyMessageListener(2, 1, 1);
        verifyRedisEntry(query, jobId, List.of(JobStatus.FINISHED), 1, true);
        verifyIdsAndResultFiles(jobId);
    }

    @Test
    void sendAndProcessMessageFailAfterMaximumRetry() throws IOException {
        String query = "accession:P12345";
        UniProtKBStreamRequest request = createStreamRequest(query);
        String jobId = this.hashGenerator.generateHash(request);
        when(this.uniProtKBMessageListener.streamIds(request))
                .thenThrow(
                        new MessageListenerException(
                                "Forced exception in streamIds to test max retry"));
        // send request to queue
        sendMessage(request, jobId);
        // Producer
        verify(this.messageService, never()).logAlreadyProcessed(jobId);
        await().until(jobCreatedInRedis(jobId));
        await().until(jobErrored(jobId));
        await().until(jobRetriedMaximumTimes(jobId));
        // verify  redis
        verifyRedisEntry(query, jobId, List.of(JobStatus.ERROR), this.maxRetry, true);
        // after certain delay the job should be reprocessed
        await().until(getMessageCountInQueue(this.rejectedQueue), equalTo(1));
        verifyMessageListener(3, 3, 3);
        verifyRedisEntry(query, jobId, List.of(JobStatus.ERROR), this.maxRetry, true);
        verifyIdsAndResultFilesDoNotExist(jobId);
    }

    @Test
    void sendAndProcessMessageWithUnhandledExceptionShouldBeDiscarded()
            throws InterruptedException, IOException {
        // when
        String query = "field:value";
        UniProtKBStreamRequest request = new UniProtKBStreamRequest();
        request.setQuery(query);
        String jobId = this.hashGenerator.generateHash(request);
        IllegalArgumentException ile =
                new IllegalArgumentException(
                        "Forced exception in streamIds to test max retry with unhandled exception");
        when(this.uniProtKBMessageListener.streamIds(request)).thenThrow(ile);
        doThrow(new MessageListenerException("Forcing to throw unexpected exception"))
                .when(this.uniProtKBMessageListener)
                .dummyMethodForTesting(jobId, JobStatus.ERROR);
        sendMessage(request, jobId);
        verify(this.messageService, never()).logAlreadyProcessed(jobId);
        await().until(jobCreatedInRedis(jobId));
        await().until(jobErrored(jobId));
        await().until(getJobRetriedCount(jobId), Matchers.equalTo(2));
        await().until(getMessageCountInQueue(this.rejectedQueue), equalTo(1));
        verify(this.uniProtKBMessageListener, atLeast(3)).onMessage(any());
        verifyRedisEntry(query, jobId, List.of(JobStatus.ERROR), 2, true);
        verifyIdsAndResultFilesDoNotExist(jobId);
    }

    private void sendMessage(UniProtKBStreamRequest request, String jobId) {
        MessageProperties messageHeader = new MessageProperties();
        messageHeader.setHeader(JOB_ID, jobId);
        String contentType = "application/json";
        messageHeader.setHeader(CONTENT_TYPE, contentType);
        this.messageService.sendMessage(request, messageHeader);
    }

    private UniProtKBStreamRequest createStreamRequest(String query) {
        UniProtKBStreamRequest request = new UniProtKBStreamRequest();
        request.setQuery(query);
        return request;
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

    private void verifyMessageListener(int timesOnMessage, int timesAddHeader, int timesStreamIds) {
        //        await().atMost(20, SECONDS).until(getMessageCountInQueue(downloadQueue),
        // equalTo(0));
        verify(this.uniProtKBMessageListener, atLeast(timesOnMessage)).onMessage(any());
        verify(this.uniProtKBMessageListener, atLeast(timesAddHeader))
                .addAdditionalHeaders(any(), any());
        verify(this.uniProtKBMessageListener, atLeast(timesStreamIds)).streamIds(any());
        verify(this.uniProtKBMessageListener, never()).setMaxRetryCount(any());
    }

    private Callable<Boolean> jobCreatedInRedis(String jobId) {
        return () -> this.downloadJobRepository.existsById(jobId);
    }

    private Callable<Boolean> jobFinished(String jobId) {
        return () ->
                this.downloadJobRepository.existsById(jobId)
                        && this.downloadJobRepository.findById(jobId).get().getStatus()
                                == JobStatus.FINISHED;
    }

    private Callable<Boolean> jobErrored(String jobId) {
        return () ->
                this.downloadJobRepository.existsById(jobId)
                        && this.downloadJobRepository.findById(jobId).get().getStatus()
                                == JobStatus.ERROR;
    }

    private Callable<Boolean> jobRetriedMaximumTimes(String jobId) {
        return () ->
                this.downloadJobRepository.existsById(jobId)
                        && this.downloadJobRepository.findById(jobId).get().getRetried()
                                == this.maxRetry;
    }

    private Callable<Integer> getJobRetriedCount(String jobId) {
        return () -> this.downloadJobRepository.findById(jobId).get().getRetried();
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

    private void verifyIdsAndResultFilesDoNotExist(String jobId) throws IOException {
        // verify the ids file
        Path idsFilePath = Path.of(this.idsFolder + "/" + jobId);
        Assertions.assertTrue(Files.notExists(idsFilePath));
        // verify result file
        Path resultFilePath = Path.of(this.resultFolder + "/" + jobId);
        Assertions.assertTrue(Files.notExists(resultFilePath));
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
