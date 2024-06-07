package org.uniprot.api.async.download.mq;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.uniprot.api.async.download.messaging.config.common.AsyncDownloadQueueConfigProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessagingService {
    private static final String JOB_ID_HEADER = "jobId";
    private final AsyncDownloadQueueConfigProperties queueConfigProperties;
    private final RabbitTemplate rabbitTemplate;

    public MessagingService(
            AsyncDownloadQueueConfigProperties queueConfigProperties,
            RabbitTemplate rabbitTemplate) {
        this.queueConfigProperties = queueConfigProperties;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(Message message) {
        rabbitTemplate.send(message);
    }

    public void send(Message message, String exchangeName) {
        rabbitTemplate.convertAndSend(exchangeName, message);
    }

    public void send(Message message, String exchangeName, String routingKey) {
        rabbitTemplate.send(exchangeName, routingKey, message);
    }

    public void sendToRetry(Message message) {
        send(message, queueConfigProperties.getRetryQueueName());
        log.info(
                "Message with jobId {} sent to retry queue {}",
                message.getMessageProperties().getHeader(JOB_ID_HEADER),
                queueConfigProperties.getRetryQueueName());
    }

    public void sendToRejected(Message message) {
        send(message, queueConfigProperties.getRejectedQueueName());
        log.info(
                "Message with jobId {} sent to rejected queue {}",
                message.getMessageProperties().getHeader(JOB_ID_HEADER),
                queueConfigProperties.getRejectedQueueName());
    }

    public int getMaxRetryCount() {
        return queueConfigProperties.getRetryMaxCount();
    }
}
