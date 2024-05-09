package org.uniprot.api.async.download.messaging.config.idmapping;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadRequestToArrayConverter;
import org.uniprot.api.rest.request.HashGenerator;

/**
 * Initialisation code for Rabbit MQ to be used by both producer and consumer
 *
 * @author sahmad
 * @created 23/11/2022
 */
@Configuration
public class IdMappingRabbitMQConfig {
    @Bean
    public Exchange idMappingDownloadExchange(
            IdMappingAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return ExchangeBuilder.directExchange(asyncDownloadQConfigProps.getExchangeName())
                .durable(asyncDownloadQConfigProps.isDurable())
                .build();
    }

    @Bean
    public Queue idMappingDownloadQueue(
            IdMappingAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return QueueBuilder.durable(asyncDownloadQConfigProps.getQueueName())
                .deadLetterExchange(asyncDownloadQConfigProps.getExchangeName())
                .deadLetterRoutingKey(asyncDownloadQConfigProps.getRetryQueueName())
                .ttl(asyncDownloadQConfigProps.getTtlInMillis())
                .quorum()
                .build();
    }

    @Bean
    Queue idMappingRetryQueue(
            IdMappingAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return QueueBuilder.durable(asyncDownloadQConfigProps.getRetryQueueName())
                .ttl(asyncDownloadQConfigProps.getRetryDelayInMillis())
                .deadLetterExchange(asyncDownloadQConfigProps.getExchangeName())
                .deadLetterRoutingKey(asyncDownloadQConfigProps.getRoutingKey())
                .quorum()
                .build();
    }

    // queue where failed messages after maximum retries will end up
    @Bean
    Queue idMappingUndeliveredQueue(
            IdMappingAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return QueueBuilder.durable(asyncDownloadQConfigProps.getRejectedQueueName())
                .quorum()
                .build();
    }

    @Bean
    public Binding idMappingDownloadBinding(
            Queue idMappingDownloadQueue,
            Exchange idMappingDownloadExchange,
            IdMappingAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return BindingBuilder.bind(idMappingDownloadQueue)
                .to((DirectExchange) idMappingDownloadExchange)
                .with(asyncDownloadQConfigProps.getRoutingKey());
    }

    @Bean
    Binding idMappingRetryBinding(Queue idMappingRetryQueue, Exchange idMappingDownloadExchange) {
        return BindingBuilder.bind(idMappingRetryQueue)
                .to((DirectExchange) idMappingDownloadExchange)
                .with(idMappingRetryQueue.getName());
    }

    @Bean
    Binding idMappingUndeliveredBinding(
            Queue idMappingUndeliveredQueue, Exchange idMappingDownloadExchange) {
        return BindingBuilder.bind(idMappingUndeliveredQueue)
                .to((DirectExchange) idMappingDownloadExchange)
                .with(idMappingUndeliveredQueue.getName());
    }

    @Bean
    public HashGenerator<IdMappingDownloadRequest> asyncIdMappingHashGenerator(
            @Value("${async.download.idmapping.hash.salt}") String hashSalt) {
        return new HashGenerator<>(new IdMappingDownloadRequestToArrayConverter(), hashSalt);
    }
}
