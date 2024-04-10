package org.uniprot.api.async.download.refactor.messaging;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.uniprot.api.async.download.messaging.config.common.AsyncDownloadQueueConfigProperties;

public class MessagingService {
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

    public void sendToRetry(Message message) {
        rabbitTemplate.convertAndSend(queueConfigProperties.getRetryQueueName(), message);
    }

    public void sendToRejected(Message message) {
        rabbitTemplate.convertAndSend(queueConfigProperties.getRejectedQueueName(), message);
    }

    public int getMaxRetryCount() {
        return queueConfigProperties.getRetryMaxCount();
    }
}
