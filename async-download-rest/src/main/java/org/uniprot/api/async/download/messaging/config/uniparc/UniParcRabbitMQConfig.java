package org.uniprot.api.async.download.messaging.config.uniparc;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.async.download.messaging.config.common.QueueConsumerConfigUtils;
import org.uniprot.api.async.download.messaging.consumer.uniparc.UniParcMessageConsumer;
import org.uniprot.api.async.download.model.request.DownloadRequestToArrayConverter;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.rest.request.HashGenerator;

/**
 * Initialisation code for Rabbit MQ to be used by both producer and consumer
 *
 * @author sahmad
 * @created 23/11/2022
 */
@Configuration
public class UniParcRabbitMQConfig {

    @Bean
    public Exchange uniParcDownloadExchange(
            UniParcAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return ExchangeBuilder.directExchange(asyncDownloadQConfigProps.getExchangeName())
                .durable(asyncDownloadQConfigProps.isDurable())
                .build();
    }

    @Bean
    public Queue uniParcDownloadQueue(
            UniParcAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return QueueBuilder.durable(asyncDownloadQConfigProps.getQueueName())
                .deadLetterExchange(asyncDownloadQConfigProps.getExchangeName())
                .deadLetterRoutingKey(asyncDownloadQConfigProps.getRetryQueueName())
                .ttl(asyncDownloadQConfigProps.getTtlInMillis())
                .quorum()
                .build();
    }

    @Bean
    Queue uniParcRetryQueue(UniParcAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return QueueBuilder.durable(asyncDownloadQConfigProps.getRetryQueueName())
                .ttl(asyncDownloadQConfigProps.getRetryDelayInMillis())
                .deadLetterExchange(asyncDownloadQConfigProps.getExchangeName())
                .deadLetterRoutingKey(asyncDownloadQConfigProps.getRoutingKey())
                .quorum()
                .build();
    }

    // queue where failed messages after maximum retries will end up
    @Bean
    Queue uniParcUndeliveredQueue(
            UniParcAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return QueueBuilder.durable(asyncDownloadQConfigProps.getRejectedQueueName())
                .quorum()
                .build();
    }

    @Bean
    public Binding uniParcDownloadBinding(
            Queue uniParcDownloadQueue,
            Exchange uniParcDownloadExchange,
            UniParcAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return BindingBuilder.bind(uniParcDownloadQueue)
                .to((DirectExchange) uniParcDownloadExchange)
                .with(asyncDownloadQConfigProps.getRoutingKey());
    }

    @Bean
    Binding uniParcRetryBinding(Queue uniParcRetryQueue, Exchange uniParcDownloadExchange) {
        return BindingBuilder.bind(uniParcRetryQueue)
                .to((DirectExchange) uniParcDownloadExchange)
                .with(uniParcRetryQueue.getName());
    }

    @Bean
    Binding uniParcUndeliveredBinding(
            Queue uniParcUndeliveredQueue, Exchange uniParcDownloadExchange) {
        return BindingBuilder.bind(uniParcUndeliveredQueue)
                .to((DirectExchange) uniParcDownloadExchange)
                .with(uniParcUndeliveredQueue.getName());
    }

    @Bean
    public MessageListenerContainer uniParcMessageListenerContainer(
            ConnectionFactory connectionFactory,
            UniParcMessageConsumer uniParcMessageConsumer,
            UniParcAsyncDownloadQueueConfigProperties configProps) {

        return QueueConsumerConfigUtils.getSimpleMessageListenerContainer(
                connectionFactory, uniParcMessageConsumer, configProps);
    }

    @Bean
    public HashGenerator<UniParcDownloadRequest> uniParcDownloadHashGenerator(
            @Value("${async.download.uniparc.hash.salt}") String hashSalt) {
        return new HashGenerator<>(new DownloadRequestToArrayConverter<>(), hashSalt);
    }
}
