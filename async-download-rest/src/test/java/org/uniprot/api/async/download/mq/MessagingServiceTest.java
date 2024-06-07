package org.uniprot.api.async.download.mq;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.uniprot.api.async.download.messaging.config.common.AsyncDownloadQueueConfigProperties;

public abstract class MessagingServiceTest {
    public static final String RETRY_QUEUE = "retryQueue";
    public static final String REJECTED_QUEUE = "rejectedQueue";
    public static final int MAX_RETRY = 7;
    public static final String JOB_ID = "jobId";
    public static final String SOME_JOB_ID = "someJobId";
    public static final String EXCHANGE = "exchange";
    public static final String ROUTING_KEY = "routingKey";
    @Mock private Message message;
    @Mock private MessageProperties messageProperties;
    protected AsyncDownloadQueueConfigProperties queueConfigProperties;
    protected RabbitTemplate rabbitTemplate;
    protected MessagingService messagingService;

    @Test
    void send() {
        messagingService.send(message);

        verify(rabbitTemplate).send(message);
    }

    @Test
    void sendWithRoutingKey() {
        messagingService.send(message, EXCHANGE, ROUTING_KEY);

        verify(rabbitTemplate).send(EXCHANGE, ROUTING_KEY, message);
    }

    @Test
    void sendToExchange() {
        messagingService.send(message, EXCHANGE);

        verify(rabbitTemplate).convertAndSend(EXCHANGE, message);
    }

    @Test
    void sendToRetry() {
        when(queueConfigProperties.getRetryQueueName()).thenReturn(RETRY_QUEUE);
        mockMessageProperties();

        messagingService.sendToRetry(message);

        verify(rabbitTemplate).convertAndSend(RETRY_QUEUE, message);
    }

    private void mockMessageProperties() {
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getHeader(JOB_ID)).thenReturn(SOME_JOB_ID);
    }

    @Test
    void sendToRejected() {
        when(queueConfigProperties.getRejectedQueueName()).thenReturn(REJECTED_QUEUE);
        mockMessageProperties();

        messagingService.sendToRejected(message);

        verify(rabbitTemplate).convertAndSend(REJECTED_QUEUE, message);
    }

    @Test
    void getMaxRetryCount() {
        when(queueConfigProperties.getRetryMaxCount()).thenReturn(MAX_RETRY);

        assertSame(MAX_RETRY, messagingService.getMaxRetryCount());
    }
}
