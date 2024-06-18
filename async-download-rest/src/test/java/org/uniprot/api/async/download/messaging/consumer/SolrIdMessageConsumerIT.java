package org.uniprot.api.async.download.messaging.consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.SolrStreamDownloadRequest;
import org.uniprot.api.common.repository.stream.rdf.RdfEntryCountProvider;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.context.FileType;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static org.uniprot.api.async.download.messaging.consumer.ContentBasedAndRetriableMessageConsumer.CURRENT_RETRIED_COUNT_HEADER;
import static org.uniprot.api.rest.download.model.JobStatus.*;
import static org.uniprot.api.rest.output.UniProtMediaType.SUPPORTED_RDF_MEDIA_TYPES;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class SolrIdMessageConsumerIT<
        T extends SolrStreamDownloadRequest, R extends DownloadJob>
        extends BasicMessageConsumerIT {
    public static final String ID = "someId";
    protected static final String JOB_ID_HEADER = "jobId";
    public static final int MAX_RETRY_COUNT = 3;
    public static final long UPDATE_COUNT = 10;
    private static final long PROCESSED_ENTRIES = 23;
    protected ContentBasedAndRetriableMessageConsumer<T, R> messageConsumer;
    protected DownloadJobRepository<R> downloadJobRepository;
    protected AsyncDownloadFileHandler asyncDownloadFileHandler;
    protected DownloadConfigProperties downloadConfigProperties;
    protected T downloadRequest;
    @Autowired
    protected MessageConverter messageConverter;
    @MockBean
    protected RdfEntryCountProvider rdfEntryCountProvider;

    @AfterEach
    void tearDown() {
        asyncDownloadFileHandler.deleteAllFiles(ID);
    }

    @Test
    void onMessage_maxRetriesReached() {
        MessageProperties messageHeader = new MessageProperties();
        messageHeader.setHeader(JOB_ID_HEADER, ID);
        messageHeader.setHeader(CURRENT_RETRIED_COUNT_HEADER, MAX_RETRY_COUNT);
        Message message = new Message("body".getBytes(), messageHeader);
        saveDownloadJob(ID, MAX_RETRY_COUNT, ERROR, UPDATE_COUNT, PROCESSED_ENTRIES);

        messageConsumer.onMessage(message);

        R job = downloadJobRepository.findById(ID).get();
        assertEquals(ERROR, job.getStatus());
        assertEquals(MAX_RETRY_COUNT, job.getRetried());
        assertEquals(0, job.getUpdateCount());
        assertEquals(0, job.getProcessedEntries());
        assertFalse(asyncDownloadFileHandler.areAllFilesExist(ID));
    }

    @Test
    void onMessage_jobCurrentlyRunning() throws Exception {
        createIdAndResultFiles(ID);
        MessageProperties messageHeader = new MessageProperties();
        messageHeader.setHeader(JOB_ID_HEADER, ID);
        Message message = new Message("body".getBytes(), messageHeader);
        saveDownloadJob(ID, 0, RUNNING, UPDATE_COUNT, PROCESSED_ENTRIES);

        messageConsumer.onMessage(message);

        R job = downloadJobRepository.findById(ID).get();
        assertEquals(RUNNING, job.getStatus());
        assertEquals(0, job.getRetried());
        assertEquals(UPDATE_COUNT, job.getUpdateCount());
        assertTrue(asyncDownloadFileHandler.areAllFilesExist(ID));
    }

    @Test
    void onMessage_alreadyFinished() throws Exception {
        createIdAndResultFiles(ID);
        MessageProperties messageHeader = new MessageProperties();
        messageHeader.setHeader(JOB_ID_HEADER, ID);
        Message message = new Message("body".getBytes(), messageHeader);
        saveDownloadJob(ID, 0, FINISHED, UPDATE_COUNT, PROCESSED_ENTRIES);

        messageConsumer.onMessage(message);

        R job = downloadJobRepository.findById(ID).get();
        assertEquals(FINISHED, job.getStatus());
        assertEquals(0, job.getRetried());
        assertEquals(UPDATE_COUNT, job.getUpdateCount());
        assertEquals(PROCESSED_ENTRIES, job.getProcessedEntries());
        assertTrue(asyncDownloadFileHandler.areAllFilesExist(ID));
    }

    @Test
    void onMessage_retryLoop() throws Exception {
        createIdAndResultFiles(ID);
        MessageProperties messageHeader = new MessageProperties();
        messageHeader.setHeader(JOB_ID_HEADER, ID);
        messageHeader.setHeader(CURRENT_RETRIED_COUNT_HEADER, 1);
        Message message = new Message("body".getBytes(), messageHeader);
        saveDownloadJob(ID, 1, ERROR, UPDATE_COUNT, PROCESSED_ENTRIES);

        messageConsumer.onMessage(message);

        await().until(retryCount(ID, MAX_RETRY_COUNT));
        await().atLeast(5, TimeUnit.SECONDS);
        R job = downloadJobRepository.findById(ID).get();
        assertEquals(ERROR, job.getStatus());
        assertEquals(MAX_RETRY_COUNT, job.getRetried());
        assertEquals(0, job.getUpdateCount());
        assertEquals(0, job.getProcessedEntries());
        assertFalse(asyncDownloadFileHandler.areAllFilesExist(ID));
    }

    @ParameterizedTest(name = "[{index}] format {0}")
    @MethodSource("getSupportedFormats")
    void onMessage_successOnFirstAttempt(String format) throws Exception {
        MessageProperties messageHeader = new MessageProperties();
        messageHeader.setHeader(JOB_ID_HEADER, ID);
        downloadRequest.setFormat(format);
        Message message = messageConverter.toMessage(downloadRequest, messageHeader);
        saveDownloadJob(ID, 0, NEW, 0, 0);
        if (isRdfType(format)) {
            when(rdfEntryCountProvider.getEntryCount(anyString(), anyString(), anyString())).thenReturn(12);
        }

        messageConsumer.onMessage(message);

        R job = downloadJobRepository.findById(ID).get();
        assertEquals(0, job.getRetried());
        assertEquals(12, job.getTotalEntries());
        assertFalse(asyncDownloadFileHandler.areAllFilesExist(ID));
        assertJobSpecifics(job, format);
        verifyIdsFiles(ID);
        if (Objects.equals(format,"json")) {
            verifyResultFile(ID);
        }
    }

    protected abstract void verifyIdsFiles(String id) throws Exception;

    protected abstract void verifyResultFile(String id) throws Exception;

    protected abstract void assertJobSpecifics(R job, String format);

    protected boolean isRdfType(String format) {
        return SUPPORTED_RDF_MEDIA_TYPES.keySet().stream().map(Objects::toString).collect(Collectors.toSet()).contains(format);
    }

    @Test
    void onMessage_successOnRemainingAttempt() throws Exception {
        createIdAndResultFiles(ID);
        MessageProperties messageHeader = new MessageProperties();
        messageHeader.setHeader(JOB_ID_HEADER, ID);
        messageHeader.setHeader(CURRENT_RETRIED_COUNT_HEADER, 1);
        Message message = messageConverter.toMessage(downloadRequest, messageHeader);
        saveDownloadJob(ID, 1, ERROR, UPDATE_COUNT, PROCESSED_ENTRIES);

        messageConsumer.onMessage(message);

        R job = downloadJobRepository.findById(ID).get();
        assertEquals(FINISHED, job.getStatus());
        assertEquals(1, job.getRetried());
        assertEquals(12, job.getTotalEntries());
        assertEquals(12, job.getProcessedEntries());
        assertFalse(asyncDownloadFileHandler.areAllFilesExist(ID));
        assertJobSpecifics(job, "json");
    }

    private Callable<Boolean> retryCount(String id, int maxRetryCount) {
        return () -> (getRetryCount(id) == maxRetryCount);
    }

    private int getRetryCount(String id) {
        return downloadJobRepository.findById(id).get().getRetried();
    }

    private Callable<Boolean> hasJobComeToStatus(String id, JobStatus jobStatus) {
        return () -> (getJobStatus(id).equals(jobStatus));
    }

    private JobStatus getJobStatus(String id) {
        return downloadJobRepository.findById(id).get().getStatus();
    }

    private void createIdAndResultFiles(String id) throws Exception {
        createIdFile(id);
        createResultFile(id);
    }

    @Override
    protected DownloadJobRepository<R> getDownloadJobRepository() {
        return downloadJobRepository;
    }

    private void createIdFile(String jobId) throws Exception {
        createFile(downloadConfigProperties.getIdFilesFolder(), jobId);
    }

    private void createFile(String folder, String fileName) throws Exception {
        Path idsFile = Paths.get(folder, fileName);
        BufferedWriter writer =
                Files.newBufferedWriter(
                        idsFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        writer.append("");
    }

    private void createResultFile(String jobId) throws Exception {
        createFile(downloadConfigProperties.getResultFilesFolder(), jobId + "." + FileType.GZIP.getExtension());
    }

    protected abstract void saveDownloadJob(String id, int retryCount, JobStatus jobStatus, long updateCount, long processedEntries);

    protected abstract Stream<Arguments> getSupportedFormats();
}
