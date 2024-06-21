package org.uniprot.api.async.download.messaging.consumer;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.uniprot.api.async.download.messaging.consumer.ContentBasedAndRetriableMessageConsumer.*;
import static org.uniprot.api.rest.download.model.JobStatus.FINISHED;
import static org.uniprot.api.rest.download.model.JobStatus.RUNNING;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.async.download.mq.MessagingService;
import org.uniprot.api.async.download.service.JobService;
import org.uniprot.api.rest.download.model.JobStatus;

public abstract class ContentBasedAndRetriableMessageConsumerTest<
        T extends DownloadRequest, R extends DownloadJob> {
    protected static final String ID = "someId";
    private static final int RETRIES = 7;
    private static final int RETRIES_EXCEEDED = 17;
    private static final int MAX_RETRIES = 10;
    protected MessagingService messagingService;
    protected RequestProcessor<T> requestProcessor;
    protected AsyncDownloadFileHandler asyncDownloadFileHandler;
    protected JobService<R> jobService;
    protected MessageConverter messageConverter;
    protected ContentBasedAndRetriableMessageConsumer<T, R> messageConsumer;
    @Mock private LocalDateTime dateTime;
    @Mock protected Message message;
    @Mock private MessageProperties messageProperties;
    protected R downloadJob;
    protected T downloadRequest;

    protected void mockCommon() {
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getHeader(JOB_ID_HEADER)).thenReturn(ID);
        when(messagingService.getMaxRetryCount()).thenReturn(MAX_RETRIES);
    }

    @Test
    void onMessage_maxRetriesDone() {
        when(messageProperties.getHeader(JOB_ID_HEADER)).thenReturn(ID);
        when(messageProperties.getHeader(CURRENT_RETRIED_COUNT_HEADER))
                .thenReturn(RETRIES_EXCEEDED);
        MockedStatic<LocalDateTime> localTimeMockedStatic = mockStatic(LocalDateTime.class);
        localTimeMockedStatic.when(LocalDateTime::now).thenReturn(dateTime);

        messageConsumer.onMessage(message);

        verify(messagingService).sendToRejected(message);
        verify(jobService)
                .update(ID, Map.of(UPDATE_COUNT, 0L, UPDATED, dateTime, PROCESSED_ENTRIES, 0L));
        verify(asyncDownloadFileHandler).deleteAllFiles(ID);

        localTimeMockedStatic.reset();
        localTimeMockedStatic.close();
    }

    @Test
    void onMessage_alreadyRunningJob() {
        when(messageProperties.getHeader(JOB_ID_HEADER)).thenReturn(ID);
        when(messageProperties.getHeader(CURRENT_RETRIED_COUNT_HEADER)).thenReturn(RETRIES);
        when(jobService.find(ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getId()).thenReturn(ID);
        mockFileExistence();
        when(downloadJob.getStatus()).thenReturn(RUNNING);

        messageConsumer.onMessage(message);

        verifyNoInteractions(requestProcessor);
    }

    protected void mockFileExistence() {
        when(asyncDownloadFileHandler.areAllFilesExist(ID)).thenReturn(true);
    }

    @Test
    void onMessage_CompletedJob() {
        when(messageProperties.getHeader(JOB_ID_HEADER)).thenReturn(ID);
        when(messageProperties.getHeader(CURRENT_RETRIED_COUNT_HEADER)).thenReturn(RETRIES);
        when(jobService.find(ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getId()).thenReturn(ID);
        mockFileExistence();
        when(downloadJob.getStatus()).thenReturn(FINISHED);

        messageConsumer.onMessage(message);

        verifyNoInteractions(requestProcessor);
    }

    @Test
    void onMessage_invalidJobId() {
        when(messageProperties.getHeader(JOB_ID_HEADER)).thenReturn(ID);
        when(messageProperties.getHeader(CURRENT_RETRIED_COUNT_HEADER)).thenReturn(RETRIES);
        when(jobService.find(ID)).thenReturn(Optional.empty());
        when(message.getBody()).thenReturn("body".getBytes());

        messageConsumer.onMessage(message);

        verify(jobService)
                .update(
                        ID,
                        Map.of(
                                ContentBasedAndRetriableMessageConsumer.RETRIED,
                                8,
                                STATUS,
                                JobStatus.ERROR));
        verify(messagingService)
                .sendToRetry(
                        argThat(
                                msg ->
                                        msg.getMessageProperties()
                                                .getHeader(CURRENT_RETRIED_COUNT_HEADER)
                                                .equals(RETRIES + 1)));
    }

    @Test
    void onMessage_firstAttempt() {
        when(messageProperties.getHeader(JOB_ID_HEADER)).thenReturn(ID);
        when(messageProperties.getHeader(CURRENT_RETRIED_COUNT_HEADER)).thenReturn(0);
        when(jobService.find(ID)).thenReturn(Optional.of(downloadJob));
        when(messageConverter.fromMessage(message)).thenReturn(downloadRequest);

        messageConsumer.onMessage(message);

        InOrder inOrder = inOrder(jobService, downloadRequest, requestProcessor);
        inOrder.verify(downloadRequest).setId(ID);
        inOrder.verify(requestProcessor).process(downloadRequest);
    }

    @Test
    void onMessage_remainingAttempts() {
        when(messageProperties.getHeader(JOB_ID_HEADER)).thenReturn(ID);
        when(messageProperties.getHeader(CURRENT_RETRIED_COUNT_HEADER)).thenReturn(RETRIES);
        when(jobService.find(ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getRetried()).thenReturn(RETRIES);
        when(downloadJob.getId()).thenReturn(ID);
        when(messageConverter.fromMessage(message)).thenReturn(downloadRequest);
        MockedStatic<LocalDateTime> localTimeMockedStatic = mockStatic(LocalDateTime.class);
        localTimeMockedStatic.when(LocalDateTime::now).thenReturn(dateTime);

        messageConsumer.onMessage(message);

        InOrder inOrder =
                inOrder(jobService, asyncDownloadFileHandler, downloadRequest, requestProcessor);
        inOrder.verify(jobService).find(ID);
        inOrder.verify(jobService)
                .update(ID, Map.of(UPDATE_COUNT, 0L, UPDATED, dateTime, PROCESSED_ENTRIES, 0L));
        inOrder.verify(asyncDownloadFileHandler).deleteAllFiles(ID);
        inOrder.verify(downloadRequest).setId(ID);
        inOrder.verify(requestProcessor).process(downloadRequest);

        localTimeMockedStatic.reset();
        localTimeMockedStatic.close();
    }
}
