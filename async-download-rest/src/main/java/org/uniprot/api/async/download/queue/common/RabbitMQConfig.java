package org.uniprot.api.async.download.queue.common;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.async.download.queue.idmapping.IdMappingAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.queue.uniprotkb.UniProtKBAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.queue.uniref.UniRefAsyncDownloadQueueConfigProperties;

/**
 * Initialisation code for Rabbit MQ to be used by both producer and consumer
 *
 * @author sahmad
 * @created 23/11/2022
 */
@Configuration
@Profile({"asyncDownload"})
public class RabbitMQConfig {

    @Bean
    public RabbitMQConfigProperties rabbitMQConfigProperties() {
        return new RabbitMQConfigProperties();
    }

    @Bean
    public UniProtKBAsyncDownloadQueueConfigProperties
            uniProtKBAsyncDownloadQueueConfigProperties() {
        return new UniProtKBAsyncDownloadQueueConfigProperties();
    }

    @Bean
    public IdMappingAsyncDownloadQueueConfigProperties
            idMappingAsyncDownloadQueueConfigProperties() {
        return new IdMappingAsyncDownloadQueueConfigProperties();
    }

    @Bean
    public UniRefAsyncDownloadQueueConfigProperties uniRefAsyncDownloadQueueConfigProperties() {
        return new UniRefAsyncDownloadQueueConfigProperties();
    }

    @Bean
    public ConnectionFactory connectionFactory(RabbitMQConfigProperties rabbitMQConfigProperties) {
        CachingConnectionFactory connectionFactory =
                new CachingConnectionFactory(rabbitMQConfigProperties.getHost());
        connectionFactory.setUsername(rabbitMQConfigProperties.getUser());
        connectionFactory.setPassword(rabbitMQConfigProperties.getPassword());
        connectionFactory.setPort(rabbitMQConfigProperties.getPort());
        return connectionFactory;
    }

    @Bean
    public Exchange downloadExchange(
            UniProtKBAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return ExchangeBuilder.directExchange(asyncDownloadQConfigProps.getExchangeName())
                .durable(asyncDownloadQConfigProps.isDurable())
                .build();
    }

    /**
     * Relationship among downloadQueue(DQ), retryQueue(RQ) and undeliveredQueue(UQ) : Producer
     * writes message to the exchange and the consumer receives the message from DQ. If the consumer
     * processes the message successfully then the message is removed from DQ. Else the message is
     * sent to dead letter queue(RQ) of DQ. RQ has a. no consumer b. ttl of n millis c. DLQ is DQ.
     * So after passing n millis, the message of RQ is sent to its DLQ (DQ). The consumer picks the
     * message from DQ. If the max retry of the message is not reached, the message is reprocessed.
     * Else the message is sent to UQ.
     */
    @Bean
    public Queue downloadQueue(
            UniProtKBAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return QueueBuilder.durable(asyncDownloadQConfigProps.getQueueName())
                .deadLetterExchange(asyncDownloadQConfigProps.getExchangeName())
                .deadLetterRoutingKey(asyncDownloadQConfigProps.getRetryQueueName())
                .ttl(asyncDownloadQConfigProps.getTtlInMillis())
                .quorum()
                .build();
    }

    @Bean
    Queue retryQueue(UniProtKBAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return QueueBuilder.durable(asyncDownloadQConfigProps.getRetryQueueName())
                .ttl(asyncDownloadQConfigProps.getRetryDelayInMillis())
                .deadLetterExchange(asyncDownloadQConfigProps.getExchangeName())
                .deadLetterRoutingKey(asyncDownloadQConfigProps.getRoutingKey())
                .quorum()
                .build();
    }

    // queue where failed messages after maximum retries will end up
    @Bean
    Queue undeliveredQueue(UniProtKBAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return QueueBuilder.durable(asyncDownloadQConfigProps.getRejectedQueueName())
                .quorum()
                .build();
    }

    @Bean
    public Binding downloadBinding(
            Queue downloadQueue,
            Exchange downloadExchange,
            UniProtKBAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return BindingBuilder.bind(downloadQueue)
                .to((DirectExchange) downloadExchange)
                .with(asyncDownloadQConfigProps.getRoutingKey());
    }

    @Bean
    Binding retryBinding(Queue retryQueue, Exchange downloadExchange) {
        return BindingBuilder.bind(retryQueue)
                .to((DirectExchange) downloadExchange)
                .with(retryQueue.getName());
    }

    @Bean
    Binding undeliveredBinding(Queue undeliveredQueue, Exchange downloadExchange) {
        return BindingBuilder.bind(undeliveredQueue)
                .to((DirectExchange) downloadExchange)
                .with(undeliveredQueue.getName());
    }

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        return rabbitAdmin;
    }
}
