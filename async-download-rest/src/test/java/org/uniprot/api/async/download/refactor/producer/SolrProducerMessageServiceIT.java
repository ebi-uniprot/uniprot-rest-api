package org.uniprot.api.async.download.refactor.producer;

import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
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
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.consumer.ContentBasedAndRetriableMessageConsumer;
import org.uniprot.api.async.download.refactor.request.SolrStreamDownloadRequest;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.IllegalDownloadJobSubmissionException;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.rest.output.UniProtMediaType.valueOf;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class SolrProducerMessageServiceIT<T extends SolrStreamDownloadRequest, R extends DownloadJob> extends BasicProducerMessageServiceIT{

    @Autowired
    MessageConverter converter;

    @Captor
    ArgumentCaptor<Message> messageCaptor;

    @TempDir
    private Path tempDir;

    @Test
    void sendMessage_withSuccess() {
        T request = getSuccessDownloadRequest();

        String jobId = getService().sendMessage(request);

        assertEquals("1a9848044d4910bc55dccdaa673f4713d5ab091e", jobId);
        Mockito.verify(getConsumer(), Mockito.timeout(1000).times(1)).onMessage(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        validateMessage(message, jobId, request);

        //Validate cached data
        DownloadJob downloadJob = getJobRepository().findById(jobId)
                .orElseThrow(AssertionFailedError::new);
        validateDownloadJob(jobId, downloadJob, request);
    }

    @Test
    void sendMessage_withSuccessForceAndIdleJobAllowedAndCleanResources() throws Exception{
        T request = getSuccessDownloadRequestWithForce();
        String jobId = "60ba2e259320dcb5a23f2e432c8f6bc6d8ed417f";

        // Reproduce Idle Job in Running Status in and files created
        createJobFiles(jobId);
        LocalDateTime idleSince = LocalDateTime.now().minusMinutes(20);
        R idleJob = getDownloadJob(jobId, idleSince, request);
        getJobRepository().save(idleJob);

        String jobIdResult = getService().sendMessage(request);
        assertEquals(jobIdResult, jobId);

        //Validate message received in Listener
        Mockito.verify(getConsumer(), Mockito.timeout(1000).times(1)).onMessage(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        validateMessage(message, jobId, request);

        //Validate cached data is a new Job
        DownloadJob downloadJob = getJobRepository().findById(jobId)
                .orElseThrow(AssertionFailedError::new);
        validateDownloadJob(jobId, downloadJob, request);

        //Validate idle job files were deleted
        assertFalse(getFileHandler().isIdFileExist(jobId));
        assertFalse(getFileHandler().isResultFileExist(jobId));
    }


    protected abstract R getDownloadJob(String jobId, LocalDateTime idleSince, T request);

    @Test
    void sendMessage_jobAlreadyRunningAndNotAllowed() {
        T request = getAlreadyRunningRequest();

        String jobId = "a63c4f8dd0687bf13338a98e7115984bf3e1b52d";
        R runningJob = getDownloadJob(jobId, LocalDateTime.now(), request);
        getJobRepository().save(runningJob);

        IllegalDownloadJobSubmissionException submitionError = assertThrows(IllegalDownloadJobSubmissionException.class, () -> getService().sendMessage(request));
        assertEquals("Job with id "+ jobId +" has already been submitted", submitionError.getMessage());
    }

    @Test
    void sendMessage_WithoutFormatDefaultToJson() {
        T request = getWithoutFormatRequest();

        String jobId = "712dc7afcd2514a178e887d68400421666cde5ed";
        String resultJobId = getService().sendMessage(request);
        assertEquals(jobId, resultJobId);
        request.setFormat("json");

        Mockito.verify(getConsumer(), Mockito.timeout(1000).times(1)).onMessage(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        validateMessage(message, jobId, request);
    }

    protected abstract DownloadJobRepository<R> getJobRepository();

    protected abstract ContentBasedAndRetriableMessageConsumer<T,R> getConsumer();

    protected abstract T getSuccessDownloadRequest();

    protected abstract T getSuccessDownloadRequestWithForce();

    protected abstract T getAlreadyRunningRequest();

    protected abstract T getWithoutFormatRequest();

    protected abstract SolrProducerMessageService<T,R> getService();

    protected abstract AsyncDownloadFileHandler getFileHandler();

    protected abstract DownloadConfigProperties getDownloadConfigProperties();

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

    protected void createJobFiles(String jobId) throws IOException {
        getDownloadConfigProperties().setIdFilesFolder(tempDir + File.separator + getDownloadConfigProperties().getIdFilesFolder());
        getDownloadConfigProperties().setResultFilesFolder(tempDir + File.separator +  getDownloadConfigProperties().getResultFilesFolder());
        Files.createDirectories(Path.of(getDownloadConfigProperties().getIdFilesFolder()));
        Files.createDirectories(Path.of(getDownloadConfigProperties().getResultFilesFolder()));
        assertTrue(getFileHandler().getIdFile(jobId).toFile().createNewFile());
        assertTrue(getFileHandler().getResultFile(jobId).toFile().createNewFile());
    }

}
