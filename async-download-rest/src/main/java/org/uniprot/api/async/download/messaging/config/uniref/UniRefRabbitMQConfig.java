package org.uniprot.api.async.download.messaging.config.uniref;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.async.download.messaging.config.common.QueueConsumerConfigUtils;
import org.uniprot.api.async.download.messaging.consumer.uniref.UniRefContentBasedAndRetriableMessageConsumer;
import org.uniprot.api.async.download.model.request.DownloadRequestToArrayConverter;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.rest.request.HashGenerator;

/**
 * Initialisation code for Rabbit MQ to be used by both producer and consumer
 *
 * @author sahmad
 * @created 23/11/2022
 */
@Configuration
public class UniRefRabbitMQConfig {

    @Bean
    public Exchange uniRefDownloadExchange(
            UniRefAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return ExchangeBuilder.directExchange(asyncDownloadQConfigProps.getExchangeName())
                .durable(asyncDownloadQConfigProps.isDurable())
                .build();
    }

    @Bean
    public Queue uniRefDownloadQueue(
            UniRefAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return QueueBuilder.durable(asyncDownloadQConfigProps.getQueueName())
                .deadLetterExchange(asyncDownloadQConfigProps.getExchangeName())
                .deadLetterRoutingKey(asyncDownloadQConfigProps.getRetryQueueName())
                .ttl(asyncDownloadQConfigProps.getTtlInMillis())
                .quorum()
                .build();
    }

    @Bean
    Queue uniRefRetryQueue(UniRefAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return QueueBuilder.durable(asyncDownloadQConfigProps.getRetryQueueName())
                .ttl(asyncDownloadQConfigProps.getRetryDelayInMillis())
                .deadLetterExchange(asyncDownloadQConfigProps.getExchangeName())
                .deadLetterRoutingKey(asyncDownloadQConfigProps.getRoutingKey())
                .quorum()
                .build();
    }

    // queue where failed messages after maximum retries will end up
    @Bean
    Queue uniRefUndeliveredQueue(
            UniRefAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return QueueBuilder.durable(asyncDownloadQConfigProps.getRejectedQueueName())
                .quorum()
                .build();
    }

    @Bean
    public Binding uniRefDownloadBinding(
            Queue uniRefDownloadQueue,
            Exchange uniRefDownloadExchange,
            UniRefAsyncDownloadQueueConfigProperties asyncDownloadQConfigProps) {
        return BindingBuilder.bind(uniRefDownloadQueue)
                .to((DirectExchange) uniRefDownloadExchange)
                .with(asyncDownloadQConfigProps.getRoutingKey());
    }

    @Bean
    Binding uniRefRetryBinding(Queue uniRefRetryQueue, Exchange uniRefDownloadExchange) {
        return BindingBuilder.bind(uniRefRetryQueue)
                .to((DirectExchange) uniRefDownloadExchange)
                .with(uniRefRetryQueue.getName());
    }

    @Bean
    Binding uniRefUndeliveredBinding(
            Queue uniRefUndeliveredQueue, Exchange uniRefDownloadExchange) {
        return BindingBuilder.bind(uniRefUndeliveredQueue)
                .to((DirectExchange) uniRefDownloadExchange)
                .with(uniRefUndeliveredQueue.getName());
    }

    @Bean
    public MessageListenerContainer uniRefMessageListenerContainer(
            ConnectionFactory connectionFactory,
            UniRefContentBasedAndRetriableMessageConsumer uniRefMessageConsumer,
            UniRefAsyncDownloadQueueConfigProperties configProps) {

        return QueueConsumerConfigUtils.getSimpleMessageListenerContainer(
                connectionFactory, uniRefMessageConsumer, configProps);
    }

    @Bean
    public HashGenerator<UniRefDownloadRequest> uniRefDownloadHashGenerator(
            @Value("${async.download.uniprotkb.hash.salt}") String hashSalt) {
        return new HashGenerator<>(new DownloadRequestToArrayConverter<>(), hashSalt);
    }
}
