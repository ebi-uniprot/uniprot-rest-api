package org.uniprot.api.async.download.refactor.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.uniprot.api.async.download.messaging.config.common.AsyncDownloadQueueConfigProperties;

@ExtendWith(MockitoExtension.class)
class MessagingServiceTest {
    public static final String RETRY_QUEUE = "retryQueue";
    public static final String REJECTED_QUEUE = "rejectedQueue";
    public static final int MAX_RETRY = 7;
    @Mock private Message message;
    @Mock private AsyncDownloadQueueConfigProperties queueConfigProperties;
    @Mock private RabbitTemplate rabbitTemplate;
    @InjectMocks private MessagingService messagingService;

    @Test
    void send() {
        messagingService.send(message);

        verify(rabbitTemplate).send(message);
    }

    @Test
    void sendToRetry() {
        when(queueConfigProperties.getRetryQueueName()).thenReturn(RETRY_QUEUE);

        messagingService.sendToRetry(message);

        verify(rabbitTemplate).convertAndSend(RETRY_QUEUE, message);
    }

    @Test
    void sendToRejected() {
        when(queueConfigProperties.getRejectedQueueName()).thenReturn(REJECTED_QUEUE);

        messagingService.sendToRejected(message);

        verify(rabbitTemplate).convertAndSend(REJECTED_QUEUE, message);
    }

    @Test
    void getMaxRetryCount() {
        when(queueConfigProperties.getRetryMaxCount()).thenReturn(MAX_RETRY);

        assertSame(MAX_RETRY, messagingService.getMaxRetryCount());
    }
}
