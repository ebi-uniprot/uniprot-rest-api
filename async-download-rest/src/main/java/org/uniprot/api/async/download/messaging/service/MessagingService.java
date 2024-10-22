package org.uniprot.api.async.download.messaging.service;

import org.springframework.amqp.core.Message;

public interface MessagingService {
    void send(Message message);

    void send(Message message, String exchangeName);

    void send(Message message, String exchangeName, String routingKey);

    void sendToRetry(Message message);

    void sendToRejected(Message message);

    int getMaxRetryCount();
}
