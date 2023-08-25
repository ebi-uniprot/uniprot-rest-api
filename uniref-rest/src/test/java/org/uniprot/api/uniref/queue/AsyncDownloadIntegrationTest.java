package org.uniprot.api.uniref.queue;

import static com.carrotsearch.ant.tasks.junit4.dependencies.com.google.common.base.Predicates.equalTo;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE;
import static org.uniprot.api.uniref.controller.TestUtils.uncompressFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.rest.download.AsyncDownloadTestConfig;
import org.uniprot.api.rest.download.configuration.RedisConfiguration;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.MessageListenerException;
import org.uniprot.api.rest.download.queue.ProducerMessageService;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.api.rest.request.HashGenerator;
import org.uniprot.api.uniref.UniRefRestApplication;
import org.uniprot.api.uniref.controller.AbstractUniRefDownloadIT;
import org.uniprot.api.uniref.controller.UniRefDownloadController;
import org.uniprot.api.uniref.repository.DataStoreTestConfig;
import org.uniprot.api.uniref.repository.store.UniRefStoreConfig;
import org.uniprot.api.uniref.request.UniRefDownloadRequest;
import org.uniprot.store.search.SolrCollection;

import com.jayway.jsonpath.JsonPath;

@ActiveProfiles(profiles = {"offline", "asyncDownload", "integration"})
@EnableConfigurationProperties
@PropertySource("classpath:application.properties")
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            UniRefRestApplication.class,
            UniRefStoreConfig.class,
            AsyncDownloadTestConfig.class,
            RedisConfiguration.class
        })
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebMvcTest({UniRefDownloadController.class})
@AutoConfigureWebClient
public class AsyncDownloadIntegrationTest extends AbstractUniRefDownloadIT {
    @Autowired private TupleStreamTemplate tupleStreamTemplate;
    @Autowired private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Autowired private HashGenerator<DownloadRequest> hashGenerator;

    @SpyBean private DownloadJobRepository downloadJobRepository;
    @SpyBean private UniRefMessageListener uniRefMessageListener;
    @SpyBean private ProducerMessageService messageService;
    @SpyBean private MessageConverter messageConverter;

    @Value("${async.download.retryMaxCount}")
    private int maxRetry;

    @Test
    void sendAndProcessDownloadMessageSuccessfully() throws IOException {
        String query = "identity:*";
        MediaType format = MediaType.APPLICATION_JSON;
        UniRefDownloadRequest request = createDownloadRequest(query, format);
        String jobId = this.messageService.sendMessage(request);
        verify(this.messageService, never()).alreadyProcessed(jobId);
        await().until(jobCreatedInRedis(jobId));
        await().atMost(Duration.ofSeconds(20)).until(jobFinished(jobId));
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
        String query = "uniprot_id:q9h9k5";
        MediaType format = MediaType.APPLICATION_JSON;
        UniRefDownloadRequest request = createDownloadRequest(query, format);
        String jobId = this.messageService.sendMessage(request);
        verify(this.messageService, never()).alreadyProcessed(jobId);
        await().until(jobCreatedInRedis(jobId));
        await().atMost(Duration.ofSeconds(20)).until(jobErrored(jobId));
        verifyRedisEntry(query, jobId, List.of(JobStatus.ERROR), 1, true);
        await().atMost(Duration.ofSeconds(20)).until(jobFinished(jobId));
        verifyMessageListener(2, 1, 1);
        verifyRedisEntry(query, jobId, List.of(JobStatus.FINISHED), 1, true);
        verifyIdsAndResultFiles(jobId);
    }

    @ParameterizedTest(name = "[{index}] type {0}")
    @MethodSource("getSupportedTypes")
    void sendAndProcessMessageFailAfterMaximumRetry(MediaType format, Integer rejectedMsgCount)
            throws IOException {
        String query = "id:UniRef100_UPI00001109EE";
        UniRefDownloadRequest request = createDownloadRequest(query, format);
        when(this.uniRefMessageListener.streamIds(request))
                .thenThrow(
                        new MessageListenerException(
                                "Forced exception in streamIds to test max retry"));
        String jobId = this.messageService.sendMessage(request);
        verify(this.messageService, never()).alreadyProcessed(jobId);
        await().until(jobCreatedInRedis(jobId));
        await().atMost(Duration.ofSeconds(20)).until(jobErrored(jobId));
        await().until(jobRetriedMaximumTimes(jobId));
        verifyRedisEntry(query, jobId, List.of(JobStatus.ERROR), this.maxRetry, true);
        await().until(
                        getMessageCountInQueue(this.rejectedQueue),
                        Matchers.greaterThanOrEqualTo(rejectedMsgCount));
        verifyRedisEntry(query, jobId, List.of(JobStatus.ERROR), this.maxRetry, true);
        verifyIdsAndResultFilesDoNotExist(jobId);
    }

    @Test
    void sendAndProcessMessageWithUnhandledExceptionShouldBeDiscarded() throws IOException {
        MediaType format = MediaType.APPLICATION_JSON;
        String query = "field:value";
        UniRefDownloadRequest request = new UniRefDownloadRequest();
        request.setQuery(query);
        request.setLargeSolrStreamRestricted(false);
        request.setFormat(format.toString());
        String jobId = this.hashGenerator.generateHash(request);
        IllegalArgumentException ile =
                new IllegalArgumentException(
                        "Forced exception in streamIds to test max retry with unhandled exception");
        when(this.uniRefMessageListener.streamIds(request)).thenThrow(ile);
        doThrow(new MessageListenerException("Forcing to throw unexpected exception"))
                .when(this.uniRefMessageListener)
                .dummyMethodForTesting(jobId, JobStatus.ERROR);
        this.messageService.sendMessage(request);
        verify(this.messageService, never()).alreadyProcessed(jobId);
        await().until(jobCreatedInRedis(jobId));
        await().atMost(Duration.ofSeconds(20)).until(jobErrored(jobId));
        await().until(getJobRetriedCount(jobId), Matchers.equalTo(2));
        await().until(getMessageCountInQueue(this.rejectedQueue), equalTo(1));
        verify(this.uniRefMessageListener, atLeast(3)).onMessage(any());
        verifyRedisEntry(query, jobId, List.of(JobStatus.ERROR), 2, true);
        verifyIdsAndResultFilesDoNotExist(jobId);
    }

    private UniRefDownloadRequest createDownloadRequest(String query, MediaType format) {
        UniRefDownloadRequest request = new UniRefDownloadRequest();
        request.setQuery(query);
        request.setFormat(format.toString());
        request.setLargeSolrStreamRestricted(false);
        return request;
    }

    private void verifyRedisEntry(
            String query, String jobId, List<JobStatus> statuses, int retryCount, boolean isError) {
        Optional<DownloadJob> optDownloadJob = this.downloadJobRepository.findById(jobId);
        assertTrue(optDownloadJob.isPresent());
        assertEquals(jobId, optDownloadJob.get().getId());
        assertEquals(query, optDownloadJob.get().getQuery());
        assertAll(
                () -> assertNull(optDownloadJob.get().getSort()),
                () -> assertNull(optDownloadJob.get().getFields()));
        assertEquals(retryCount, optDownloadJob.get().getRetried());
        assertTrue(statuses.contains(optDownloadJob.get().getStatus()));
        assertEquals(isError, Objects.nonNull(optDownloadJob.get().getError()));
        if (optDownloadJob.get().getStatus() == JobStatus.FINISHED) {
            assertNotNull(optDownloadJob.get().getResultFile());
            assertEquals(jobId, optDownloadJob.get().getResultFile());
        } else {
            assertNull(optDownloadJob.get().getResultFile());
        }
        assertAll(
                () -> assertNotNull(optDownloadJob.get().getCreated()),
                () -> assertNotNull(optDownloadJob.get().getUpdated()));
    }

    private void verifyMessageListener(int timesOnMessage, int timesAddHeader, int timesStreamIds) {
        verify(this.uniRefMessageListener, atLeast(timesOnMessage)).onMessage(any());
        verify(this.uniRefMessageListener, atLeast(timesAddHeader))
                .addAdditionalHeaders(any(), any());
        verify(this.uniRefMessageListener, atLeast(timesStreamIds)).streamIds(any());
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

    private Callable<Boolean> jobUnfinished(String jobId) {
        return () ->
                this.downloadJobRepository.existsById(jobId)
                        && this.downloadJobRepository.findById(jobId).get().getStatus()
                                == JobStatus.UNFINISHED;
    }

    private Callable<Boolean> jobRunning(String jobId) {
        return () ->
                this.downloadJobRepository.existsById(jobId)
                        && this.downloadJobRepository.findById(jobId).get().getStatus()
                                == JobStatus.RUNNING;
    }

    private Callable<Boolean> jobRetriedMaximumTimes(String jobId) {
        return () -> {
            Optional<DownloadJob> optJob = this.downloadJobRepository.findById(jobId);
            if (optJob.isPresent()) {
                return optJob.get().getRetried() == this.maxRetry;
            }
            return false;
        };
    }

    private Callable<Integer> getJobRetriedCount(String jobId) {
        return () -> {
            Optional<DownloadJob> optJob = this.downloadJobRepository.findById(jobId);
            if (optJob.isPresent()) {
                return optJob.get().getRetried();
            }
            return 0;
        };
    }

    private Callable<Integer> getMessageCountInQueue(String queueName) {
        return () -> (Integer) amqpAdmin.getQueueProperties(queueName).get("QUEUE_MESSAGE_COUNT");
    }

    private void verifyIdsAndResultFiles(String jobId) throws IOException {
        // verify the ids file
        verifyIdsFile(jobId);
        // verify result file
        String fileName = jobId + FileType.GZIP.getExtension();
        Path resultFilePath = Path.of(this.resultFolder + "/" + fileName);
        Assertions.assertTrue(Files.exists(resultFilePath));
        // uncompress the gz file
        Path unzippedFile = Path.of(this.resultFolder + "/" + jobId);
        uncompressFile(resultFilePath, unzippedFile);
        Assertions.assertTrue(Files.exists(unzippedFile));
        String resultsJson = Files.readString(unzippedFile);
        Assertions.assertNotNull(resultsJson);
        List<String> ids = JsonPath.read(resultsJson, "$.results.*.id");
        Assertions.assertNotNull(ids);
    }

    private void verifyIdsFile(String jobId) throws IOException {
        Path idsFilePath = Path.of(this.idsFolder + "/" + jobId);
        Assertions.assertTrue(Files.exists(idsFilePath));
        List<String> ids = Files.readAllLines(idsFilePath);
        Assertions.assertNotNull(ids);
    }

    private void verifyIdsAndResultFilesDoNotExist(String jobId) throws IOException {
        // verify the ids file
        Path idsFilePath = Paths.get(this.idsFolder, jobId);
        Assertions.assertTrue(Files.notExists(idsFilePath));
        // verify result file
        Path resultFilePath = Paths.get(this.resultFolder, jobId);
        Assertions.assertTrue(Files.notExists(resultFilePath));
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniref);
    }

    @Override
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return this.tupleStreamTemplate;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return this.facetTupleStreamTemplate;
    }

    @Override
    protected DownloadJobRepository getDownloadJobRepository() {
        return this.downloadJobRepository;
    }

    private Stream<Arguments> getSupportedTypes() {
        return Stream.of(
                Arguments.of(MediaType.APPLICATION_JSON, 1), Arguments.of(FASTA_MEDIA_TYPE, 2));
    }
}
