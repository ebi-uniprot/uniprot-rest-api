package org.uniprot.api.async.download.messaging.producer;

import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.rest.output.UniProtMediaType.valueOf;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.uniprot.api.async.download.messaging.consumer.MessageConsumer;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.SolrStreamDownloadRequest;
import org.uniprot.api.rest.download.queue.IllegalDownloadJobSubmissionException;

import junit.framework.AssertionFailedError;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class SolrProducerMessageServiceIT<
                T extends SolrStreamDownloadRequest, R extends DownloadJob>
        extends BasicProducerMessageServiceIT {

    @Captor ArgumentCaptor<Message> messageCaptor;

    @Test
    void sendMessage_withSuccess() {
        T request = getSuccessDownloadRequest();

        String jobId = getService().sendMessage(request);

        assertEquals("e0457b3cf8130f3d374bf4e889ef01586caf6a51", jobId);
        Mockito.verify(getConsumer(), Mockito.timeout(1000).times(1))
                .onMessage(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        validateMessage(message, jobId, request);

        // Validate cached data
        DownloadJob downloadJob =
                getMapDownloadJobRepository()
                        .findById(jobId)
                        .orElseThrow(AssertionFailedError::new);
        validateDownloadJob(jobId, downloadJob, request);
    }

    @Test
    void sendMessage_withSuccessForceAndIdleJobAllowedAndCleanResources() throws Exception {
        T request = getSuccessDownloadRequestWithForce();
        String jobId = "f98973831cfe2ba1a41cfb08151047f0c1a0d4f9";

        // Reproduce Idle Job in Running Status in and files created
        createJobFiles(jobId);
        LocalDateTime idleSince = LocalDateTime.now().minusMinutes(20);
        R idleJob = getDownloadJob(jobId, idleSince, request);
        getMapDownloadJobRepository().save(idleJob);

        String jobIdResult = getService().sendMessage(request);
        assertEquals(jobId, jobIdResult);

        // Validate message received in Listener
        Mockito.verify(getConsumer(), Mockito.timeout(1000).times(1))
                .onMessage(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        validateMessage(message, jobId, request);

        // Validate cached data is a new Job
        DownloadJob downloadJob =
                getMapDownloadJobRepository()
                        .findById(jobId)
                        .orElseThrow(AssertionFailedError::new);
        validateDownloadJob(jobId, downloadJob, request);

        // Validate idle job files were deleted
        assertFalse(getMapFileHandler().isIdFilePresent(jobId));
        assertFalse(getMapFileHandler().isResultFilePresent(jobId));
    }

    @Test
    void sendMessage_jobAlreadyRunningAndNotAllowed() {
        T request = getAlreadyRunningRequest();

        String jobId = "bca57aa8e6ae2c6ab771633215666a23d725c738";
        R runningJob = getDownloadJob(jobId, LocalDateTime.now(), request);
        getMapDownloadJobRepository().save(runningJob);

        IllegalDownloadJobSubmissionException submissionError =
                assertThrows(
                        IllegalDownloadJobSubmissionException.class,
                        () -> getService().sendMessage(request));
        assertEquals(
                "Job with id " + jobId + " has already been submitted",
                submissionError.getMessage());
    }

    @Test
    void sendMessage_WithoutFormatDefaultToJson() {
        T request = getWithoutFormatRequest();

        String jobId = "d93243dd48e4a179052fd42f8478a3e2b9902327";
        String resultJobId = getService().sendMessage(request);
        assertEquals(jobId, resultJobId);
        request.setFormat("json");

        Mockito.verify(getConsumer(), Mockito.timeout(1000).times(1))
                .onMessage(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        validateMessage(message, jobId, request);
    }

    protected abstract R getDownloadJob(String jobId, LocalDateTime idleSince, T request);

    protected abstract DownloadJobRepository<R> getMapDownloadJobRepository();

    protected abstract MessageConsumer<T, R> getConsumer();

    protected abstract T getSuccessDownloadRequest();

    protected abstract T getSuccessDownloadRequestWithForce();

    protected abstract T getAlreadyRunningRequest();

    protected abstract T getWithoutFormatRequest();

    protected abstract SolrProducerMessageService<T, R> getService();

    protected static void validateDownloadJob(
            String jobId, DownloadJob downloadJob, SolrStreamDownloadRequest request) {
        validateDownloadJob(jobId, downloadJob);

        assertEquals(request.getQuery(), downloadJob.getQuery());
        assertEquals(request.getFields(), downloadJob.getFields());
        assertEquals(request.getSort(), downloadJob.getSort());
        assertEquals(valueOf(request.getFormat()), valueOf(downloadJob.getFormat()));
    }

    protected void validateMessage(
            Message message, String jobId, SolrStreamDownloadRequest request) {
        validateMessage(message, jobId);

        SolrStreamDownloadRequest submittedRequest =
                (SolrStreamDownloadRequest) converter.fromMessage(message);
        assertEquals(request, submittedRequest);
    }
}
