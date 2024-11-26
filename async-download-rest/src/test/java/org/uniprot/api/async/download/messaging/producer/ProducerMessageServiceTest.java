package org.uniprot.api.async.download.messaging.producer;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.result.common.FileHandler;
import org.uniprot.api.async.download.model.JobSubmitFeedback;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.async.download.mq.MessagingService;
import org.uniprot.api.async.download.service.JobService;
import org.uniprot.api.rest.download.queue.IllegalDownloadJobSubmissionException;
import org.uniprot.api.rest.request.HashGenerator;

public abstract class ProducerMessageServiceTest<T extends DownloadRequest, R extends DownloadJob> {
    protected static final String JOB_ID = "someJobId";
    @Mock protected Message message;
    @Mock private JobSubmitFeedback feedback;
    protected T downloadRequest;
    protected JobService<R> jobService;
    protected HashGenerator<T> hashGenerator;
    protected MessageConverter messageConverter;
    protected MessagingService messagingService;
    protected FileHandler fileHandler;
    protected JobSubmissionRules<T, R> jobSubmissionRules;
    protected ProducerMessageService<T, R> producerMessageService;

    @Test
    void sendMessage_withForceAndAllowed() {
        mockDownloadRequest();
        when(feedback.isAllowed()).thenReturn(true);
        when(downloadRequest.isForce()).thenReturn(true);
        when(hashGenerator.generateHash(downloadRequest)).thenReturn(JOB_ID);
        when(jobSubmissionRules.submit(downloadRequest)).thenReturn(feedback);

        String jobId = producerMessageService.sendMessage(downloadRequest);

        assertSame(JOB_ID, jobId);
        verifyClean();
        verifyPreprocess(downloadRequest);
        verifyDownloadJob(downloadRequest);
        verifyMessageSent();
    }

    @Test
    void sendMessage_withoutForceAndAllowed() {
        mockDownloadRequest();
        when(feedback.isAllowed()).thenReturn(true);
        when(downloadRequest.isForce()).thenReturn(false);
        when(hashGenerator.generateHash(downloadRequest)).thenReturn(JOB_ID);
        when(jobSubmissionRules.submit(downloadRequest)).thenReturn(feedback);

        String jobId = producerMessageService.sendMessage(downloadRequest);

        assertSame(JOB_ID, jobId);
        verifyPreprocess(downloadRequest);
        verifyDownloadJob(downloadRequest);
        verifyMessageSent();
    }

    @Test
    void sendMessage_whenFormatIsEmptyAndAllowed() {
        mockDownloadRequestWithoutFormat();
        when(feedback.isAllowed()).thenReturn(true);
        when(downloadRequest.isForce()).thenReturn(false);
        when(hashGenerator.generateHash(downloadRequest)).thenReturn(JOB_ID);
        when(jobSubmissionRules.submit(downloadRequest)).thenReturn(feedback);

        String jobId = producerMessageService.sendMessage(downloadRequest);

        assertSame(JOB_ID, jobId);
        verify(downloadRequest).setFormat(APPLICATION_JSON_VALUE);
        verifyPreprocess(downloadRequest);
        verifyDownloadJob(downloadRequest);
        verifyMessageSent();
    }

    @Test
    void sendMessage_whenMessageSendingNotSuccessful() {
        mockDownloadRequestWithoutFormat();
        when(feedback.isAllowed()).thenReturn(true);
        when(downloadRequest.isForce()).thenReturn(false);
        when(hashGenerator.generateHash(downloadRequest)).thenReturn(JOB_ID);
        when(jobSubmissionRules.submit(downloadRequest)).thenReturn(feedback);
        doThrow(AmqpException.class).when(messagingService).send(message);

        assertThrows(
                AmqpException.class, () -> producerMessageService.sendMessage(downloadRequest));

        verify(downloadRequest).setFormat(APPLICATION_JSON_VALUE);
        verifyPreprocess(downloadRequest);
        verifyDownloadJob(downloadRequest);
        verifyMessageSent();
        verify(jobService).delete(JOB_ID);
    }

    @Test
    void sendMessage_whenNotAllowed() {
        when(hashGenerator.generateHash(downloadRequest)).thenReturn(JOB_ID);
        when(feedback.isAllowed()).thenReturn(false);
        when(jobSubmissionRules.submit(downloadRequest)).thenReturn(feedback);

        assertThrows(
                IllegalDownloadJobSubmissionException.class,
                () -> producerMessageService.sendMessage(downloadRequest));
        verify(jobService, never()).create(any());
        verify(messagingService, never()).send(any());
    }

    protected abstract void mockDownloadRequest();

    protected abstract void mockDownloadRequestWithoutFormat();

    protected abstract void verifyPreprocess(T downloadRequest);

    protected abstract void verifyDownloadJob(T request);

    private void verifyClean() {
        verify(jobService).delete(JOB_ID);
        verify(fileHandler).deleteAllFiles(JOB_ID);
    }

    private void verifyMessageSent() {
        verify(messagingService).send(message);
    }
}
