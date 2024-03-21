package org.uniprot.api.async.download.messaging.integration.common;

import static com.carrotsearch.ant.tasks.junit4.dependencies.com.google.common.base.Predicates.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static org.uniprot.api.async.download.common.RedisUtil.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.uniprot.api.async.download.common.AbstractDownloadIT;
import org.uniprot.api.async.download.messaging.listener.common.BaseAbstractMessageListener;
import org.uniprot.api.async.download.messaging.listener.common.MessageListenerException;
import org.uniprot.api.async.download.messaging.producer.common.ProducerMessageService;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.model.common.DownloadRequest;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.request.HashGenerator;

import com.jayway.jsonpath.JsonPath;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractAsyncDownloadIT extends AbstractDownloadIT {
    @Autowired protected AmqpAdmin amqpAdmin;

    @SpyBean protected MessageConverter messageConverter;

    protected abstract Stream<String> streamIds(DownloadRequest downloadRequest);

    protected abstract DownloadRequest createDownloadRequest(String query, MediaType format);

    protected abstract void verifyMessageListener(
            int timesOnMessage, int timesAddHeader, int timesStreamIds);

    protected abstract Stream<Arguments> getSupportedTypes();

    protected abstract BaseAbstractMessageListener getMessageListener();

    protected abstract String getMessageSuccessQuery();

    protected abstract String getMessageSuccessAfterRetryQuery();

    protected abstract String getMessageFailAfterMaximumRetryQuery();

    protected abstract String getMessageWithUnhandledExceptionQuery();

    protected abstract int getMessageSuccessAfterRetryCount();

    @Test
    void sendAndProcessDownloadMessageSuccessfully() throws IOException {
        String query = getMessageSuccessQuery();
        MediaType format = MediaType.APPLICATION_JSON;
        DownloadRequest request = createDownloadRequest(query, format);
        String jobId = getProducerMessageService().sendMessage(request);
        // Producer
        verify(getProducerMessageService(), never()).alreadyProcessed(jobId);
        // redis entry created
        await().until(jobCreatedInRedis(getDownloadJobRepository(), jobId));
        await().atMost(Duration.ofSeconds(20))
                .until(jobFinished(getDownloadJobRepository(), jobId));
        verifyMessageListener(1, 0, 1);
        verifyRedisEntry(query, jobId, JobStatus.FINISHED, 0, false, 12);
        verifyIdsAndResultFiles(jobId);
    }

    @Test
    void sendAndProcessMessageSuccessfullyAfterRetry() throws IOException {
        doThrow(new RuntimeException("Forced exception for testing on call converter.fromMessage"))
                .doCallRealMethod()
                .when(this.messageConverter)
                .fromMessage(any());
        String query = getMessageSuccessAfterRetryQuery();
        MediaType format = MediaType.APPLICATION_JSON;
        DownloadRequest request = createDownloadRequest(query, format);
        String jobId = this.getProducerMessageService().sendMessage(request);
        // Producer
        verify(this.getProducerMessageService(), never()).alreadyProcessed(jobId);
        await().until(jobCreatedInRedis(getDownloadJobRepository(), jobId));
        await().atMost(Duration.ofSeconds(20)).until(jobErrored(getDownloadJobRepository(), jobId));
        // verify  redis
        verifyRedisEntry(query, jobId, JobStatus.ERROR, 1, true, 0);
        // after certain delay the job should be reprocessed
        await().atMost(Duration.ofSeconds(20))
                .until(jobFinished(getDownloadJobRepository(), jobId));
        verifyMessageListener(2, 1, 1);
        verifyRedisEntry(
                query, jobId, JobStatus.FINISHED, 1, true, getMessageSuccessAfterRetryCount());
        verifyIdsAndResultFiles(jobId);
    }

    @ParameterizedTest(name = "[{index}] type {0}")
    @MethodSource("getSupportedTypes")
    void sendAndProcessMessageFailAfterMaximumRetry(MediaType format, Integer rejectedMsgCount)
            throws IOException {
        String query = getMessageFailAfterMaximumRetryQuery();
        DownloadRequest request = createDownloadRequest(query, format);
        when(streamIds(request))
                .thenThrow(
                        new MessageListenerException(
                                "Forced exception in streamIds to test max retry"));
        // send request to queue
        String jobId = this.getProducerMessageService().sendMessage(request);
        // Producer
        verify(this.getProducerMessageService(), never()).alreadyProcessed(jobId);
        await().until(jobCreatedInRedis(getDownloadJobRepository(), jobId));
        await().atMost(Duration.ofSeconds(20)).until(jobErrored(getDownloadJobRepository(), jobId));
        await().until(
                        jobRetriedMaximumTimes(
                                getDownloadJobRepository(),
                                jobId,
                                getTestAsyncConfig().getMaxRetry()));
        // verify  redis
        verifyRedisEntry(
                query, jobId, JobStatus.ERROR, getTestAsyncConfig().getMaxRetry(), true, 0);
        // after certain delay the job should be reprocessed
        await().until(
                        verifyMessageCountIsThanOrEqualToRejectedCount(
                                amqpAdmin,
                                getTestAsyncConfig().getRejectedQueue(),
                                rejectedMsgCount));
        verifyRedisEntry(
                query, jobId, JobStatus.ERROR, getTestAsyncConfig().getMaxRetry(), true, 0);
        verifyIdsAndResultFilesDoNotExist(jobId);
    }

    @Test
    void sendAndProcessMessageWithUnhandledExceptionShouldBeDiscarded() throws IOException {
        // when
        MediaType format = MediaType.APPLICATION_JSON;
        String query = getMessageWithUnhandledExceptionQuery();
        DownloadRequest request = createDownloadRequest(query, format);
        String jobId = getHashGenerator().generateHash(request);
        IllegalArgumentException ile =
                new IllegalArgumentException(
                        "Forced exception in streamIds to test max retry with unhandled exception");
        when(streamIds(request)).thenThrow(ile);
        doThrow(new MessageListenerException("Forcing to throw unexpected exception"))
                .when(getMessageListener())
                .dummyMethodForTesting(jobId, JobStatus.ERROR);
        this.getProducerMessageService().sendMessage(request);
        verify(this.getProducerMessageService(), never()).alreadyProcessed(jobId);
        await().until(jobCreatedInRedis(getDownloadJobRepository(), jobId));
        await().atMost(Duration.ofSeconds(20)).until(jobErrored(getDownloadJobRepository(), jobId));
        await().until(
                        verifyJobRetriedCountIsEqualToGivenCount(
                                getDownloadJobRepository(), jobId, 2));
        await().until(
                        getMessageCountInQueue(amqpAdmin, getTestAsyncConfig().getRejectedQueue()),
                        equalTo(1));
        verifyRedisEntry(query, jobId, JobStatus.ERROR, 2, true, 0);
        verifyIdsAndResultFilesDoNotExist(jobId);
    }

    protected void verifyRedisEntry(
            String query,
            String jobId,
            JobStatus status,
            int retryCount,
            boolean isError,
            int entryCount) {
        Optional<DownloadJob> optDownloadJob = getDownloadJobRepository().findById(jobId);
        if (optDownloadJob.isEmpty()) {
            throw new RuntimeException(String.format("No job found with id %s", jobId));
        }
        DownloadJob downloadJob = optDownloadJob.get();

        assertEquals(jobId, downloadJob.getId());
        assertEquals(query, downloadJob.getQuery());
        assertAll(
                () -> assertNull(downloadJob.getSort()), () -> assertNull(downloadJob.getFields()));
        assertEquals(retryCount, downloadJob.getRetried());
        assertEquals(downloadJob.getStatus(), status);
        assertEquals(isError, Objects.nonNull(downloadJob.getError()));
        assertEquals(entryCount, downloadJob.getTotalEntries());
        if (downloadJob.getStatus() == JobStatus.FINISHED) {
            assertEquals(entryCount, downloadJob.getProcessedEntries());
            assertNotNull(downloadJob.getResultFile());
            assertEquals(jobId, downloadJob.getResultFile());
        } else {
            assertNull(downloadJob.getResultFile());
        }
        assertAll(
                () -> assertNotNull(downloadJob.getCreated()),
                () -> assertNotNull(downloadJob.getUpdated()));
    }

    protected void verifyIdsAndResultFiles(String jobId) throws IOException {
        // verify the ids file
        verifyIdsFile(jobId);
        // verify result file
        String fileName = jobId + FileType.GZIP.getExtension();
        Path resultFilePath = Path.of(getTestAsyncConfig().getResultFolder() + "/" + fileName);
        Assertions.assertTrue(Files.exists(resultFilePath));
        // uncompress the gz file
        Path unzippedFile = Path.of(getTestAsyncConfig().getResultFolder() + "/" + jobId);
        uncompressFile(resultFilePath, unzippedFile);
        Assertions.assertTrue(Files.exists(unzippedFile));
        String resultsJson = Files.readString(unzippedFile);
        Assertions.assertNotNull(resultsJson);
        List<String> primaryAccessions = JsonPath.read(resultsJson, "$.results.*.primaryAccession");
        Assertions.assertNotNull(primaryAccessions);
    }

    protected void verifyIdsFile(String jobId) throws IOException {
        Path idsFilePath = Path.of(getTestAsyncConfig().getIdsFolder() + "/" + jobId);
        Assertions.assertTrue(Files.exists(idsFilePath));
        List<String> ids = Files.readAllLines(idsFilePath);
        Assertions.assertNotNull(ids);
    }

    protected void verifyIdsAndResultFilesDoNotExist(String jobId) throws IOException {
        // verify the ids file
        Path idsFilePath = Paths.get(getTestAsyncConfig().getIdsFolder(), jobId);
        Assertions.assertTrue(Files.notExists(idsFilePath));
        // verify result file
        Path resultFilePath = Paths.get(getTestAsyncConfig().getResultFolder(), jobId);
        Assertions.assertTrue(Files.notExists(resultFilePath));
    }

    protected abstract ProducerMessageService getProducerMessageService();

    protected abstract HashGenerator<DownloadRequest> getHashGenerator();
}
