package org.uniprot.api.async.download.messaging.config.mapto;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.async.download.messaging.config.common.QueueConsumerConfigUtils;
import org.uniprot.api.async.download.messaging.consumer.mapto.MapToMessageConsumer;
import org.uniprot.api.async.download.model.request.MapToDownloadRequestToArrayConverter;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.rest.request.HashGenerator;

/**
 * Initialisation code for Rabbit MQ to be used by both producer and consumer
 *
 * @author sahmad
 * @created 23/11/2022
 */
@Configuration
public class MapToRabbitMQConfig {

    @Bean
    public Exchange mapDownloadExchange(
            MapToAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return ExchangeBuilder.directExchange(asyncDownloadQConfigProps.getExchangeName())
                .durable(asyncDownloadQConfigProps.isDurable())
                .build();
    }

    @Bean
    public Queue mapDownloadQueue(MapToAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return QueueBuilder.durable(asyncDownloadQConfigProps.getQueueName())
                .deadLetterExchange(asyncDownloadQConfigProps.getExchangeName())
                .deadLetterRoutingKey(asyncDownloadQConfigProps.getRetryQueueName())
                .ttl(asyncDownloadQConfigProps.getTtlInMillis())
                .quorum()
                .build();
    }

    @Bean
    Queue mapRetryQueue(MapToAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return QueueBuilder.durable(asyncDownloadQConfigProps.getRetryQueueName())
                .ttl(asyncDownloadQConfigProps.getRetryDelayInMillis())
                .deadLetterExchange(asyncDownloadQConfigProps.getExchangeName())
                .deadLetterRoutingKey(asyncDownloadQConfigProps.getRoutingKey())
                .quorum()
                .build();
    }

    // queue where failed messages after maximum retries will end up
    @Bean
    Queue mapUndeliveredQueue(MapToAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return QueueBuilder.durable(asyncDownloadQConfigProps.getRejectedQueueName())
                .quorum()
                .build();
    }

    @Bean
    public Binding mapDownloadBinding(
            Queue mapDownloadQueue,
            Exchange mapDownloadExchange,
            MapToAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return BindingBuilder.bind(mapDownloadQueue)
                .to((DirectExchange) mapDownloadExchange)
                .with(asyncDownloadQConfigProps.getRoutingKey());
    }

    @Bean
    Binding mapRetryBinding(Queue mapRetryQueue, Exchange mapDownloadExchange) {
        return BindingBuilder.bind(mapRetryQueue)
                .to((DirectExchange) mapDownloadExchange)
                .with(mapRetryQueue.getName());
    }

    @Bean
    Binding mapUndeliveredBinding(Queue mapUndeliveredQueue, Exchange mapDownloadExchange) {
        return BindingBuilder.bind(mapUndeliveredQueue)
                .to((DirectExchange) mapDownloadExchange)
                .with(mapUndeliveredQueue.getName());
    }

    @Bean
    public MessageListenerContainer mapMessageListenerContainer(
            ConnectionFactory connectionFactory,
            MapToMessageConsumer mapToMessageConsumer,
            MapToAsyncDownloadQueueConfigProperties configProps) {

        return QueueConsumerConfigUtils.getSimpleMessageListenerContainer(
                connectionFactory, mapToMessageConsumer, configProps);
    }

    @Bean
    public HashGenerator<UniProtKBToUniRefDownloadRequest> mapDownloadHashGenerator(
            @Value("${async.download.map.hash.salt}") String hashSalt) {
        return new HashGenerator<>(new MapToDownloadRequestToArrayConverter<>(), hashSalt);
    }
}
