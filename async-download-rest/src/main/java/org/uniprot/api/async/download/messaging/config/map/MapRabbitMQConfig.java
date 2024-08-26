package org.uniprot.api.async.download.messaging.config.map;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.async.download.messaging.config.common.QueueConsumerConfigUtils;
import org.uniprot.api.async.download.messaging.consumer.map.MapMessageConsumer;
import org.uniprot.api.async.download.model.request.DownloadRequestToArrayConverter;
import org.uniprot.api.async.download.model.request.map.UniProtKBMapDownloadRequest;
import org.uniprot.api.rest.request.HashGenerator;

/**
 * Initialisation code for Rabbit MQ to be used by both producer and consumer
 *
 * @author sahmad
 * @created 23/11/2022
 */
@Configuration
public class MapRabbitMQConfig {

    @Bean
    public Exchange mapDownloadExchange(
            MapAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return ExchangeBuilder.directExchange(asyncDownloadQConfigProps.getExchangeName())
                .durable(asyncDownloadQConfigProps.isDurable())
                .build();
    }

    @Bean
    public Queue mapDownloadQueue(MapAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return QueueBuilder.durable(asyncDownloadQConfigProps.getQueueName())
                .deadLetterExchange(asyncDownloadQConfigProps.getExchangeName())
                .deadLetterRoutingKey(asyncDownloadQConfigProps.getRetryQueueName())
                .ttl(asyncDownloadQConfigProps.getTtlInMillis())
                .quorum()
                .build();
    }

    @Bean
    Queue mapRetryQueue(MapAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return QueueBuilder.durable(asyncDownloadQConfigProps.getRetryQueueName())
                .ttl(asyncDownloadQConfigProps.getRetryDelayInMillis())
                .deadLetterExchange(asyncDownloadQConfigProps.getExchangeName())
                .deadLetterRoutingKey(asyncDownloadQConfigProps.getRoutingKey())
                .quorum()
                .build();
    }

    // queue where failed messages after maximum retries will end up
    @Bean
    Queue mapUndeliveredQueue(MapAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return QueueBuilder.durable(asyncDownloadQConfigProps.getRejectedQueueName())
                .quorum()
                .build();
    }

    @Bean
    public Binding mapDownloadBinding(
            Queue mapDownloadQueue,
            Exchange mapDownloadExchange,
            MapAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
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
            MapMessageConsumer mapMessageConsumer,
            MapAsyncDownloadQueueConfigProperties configProps) {

        return QueueConsumerConfigUtils.getSimpleMessageListenerContainer(
                connectionFactory, mapMessageConsumer, configProps);
    }

    @Bean
    public HashGenerator<UniProtKBMapDownloadRequest> mapDownloadHashGenerator(
            @Value("${async.download.map.hash.salt}") String hashSalt) {
        return new HashGenerator<>(new DownloadRequestToArrayConverter<>(), hashSalt);
    }
}
