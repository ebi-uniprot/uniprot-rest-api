package org.uniprot.api.async.download.refactor.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.refactor.messaging.MessagingService;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;
import org.uniprot.api.async.download.refactor.service.JobService;

import org.springframework.amqp.core.Message;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.uniprot.api.async.download.refactor.consumer.ContentBasedAndRetriableMessageConsumer.CURRENT_RETRIED_COUNT_HEADER;
import static org.uniprot.api.async.download.refactor.consumer.ContentBasedAndRetriableMessageConsumer.JOB_ID_HEADER;

public abstract class ContentBasedAndRetriableMessageConsumerTest<T extends DownloadRequest, R extends DownloadJob> {
    private static final String ID = "someId";
    private static final int RETRIES = 17;
    private static final int MAX_RETRIES = 10;
    protected MessagingService messagingService;
    protected RequestProcessor<T> requestProcessor;
    protected AsyncDownloadFileHandler asyncDownloadFileHandler;
    protected JobService<R> jobService;
    protected MessageConverter messageConverter;
    protected ContentBasedAndRetriableMessageConsumer<T, R> messageConsumer;
    @Mock
    protected Message message;
    @Mock
    private MessageProperties messageProperties;

    protected void mockCommon() {
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getHeader(JOB_ID_HEADER)).thenReturn(ID);
        when(messagingService.getMaxRetryCount()).thenReturn(MAX_RETRIES);
    }

    @Test
    void onMessage_maxRetriesDone() {
        when(messageProperties.getHeader(JOB_ID_HEADER)).thenReturn(ID);
        when(messageProperties.getHeader(CURRENT_RETRIED_COUNT_HEADER)).thenReturn(RETRIES);

        messageConsumer.onMessage(message);

        verify(messagingService).sendToRejected(message);
    }

    @Test
    void onMessage_invalidJobId() {
        when(messageProperties.getHeader(JOB_ID_HEADER)).thenReturn(ID);
        when(messageProperties.getHeader(CURRENT_RETRIED_COUNT_HEADER)).thenReturn(RETRIES);

        messageConsumer.onMessage(message);

        verify(messagingService).sendToRejected(message);
    }


}