package org.uniprot.api.async.download.messaging.config.common;

import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.listener.idmapping.IdMappingMessageListener;
import org.uniprot.api.async.download.messaging.listener.uniprotkb.UniProtKBMessageListener;
import org.uniprot.api.async.download.messaging.listener.uniref.UniRefMessageListener;

/**
 * @author sahmad
 * @created 22/11/2022
 */
@Configuration
public class QueueConsumerConfig {

    @Bean
    public MessageListenerContainer idMappingMessageListenerContainer(
            ConnectionFactory connectionFactory,
            IdMappingMessageListener idMappingMessageListener,
            IdMappingAsyncDownloadQueueConfigProperties configProps) {
        return getSimpleMessageListenerContainer(
                connectionFactory, idMappingMessageListener, configProps);
    }

    @Bean
    public MessageListenerContainer uniProtKBMessageListenerContainer(
            ConnectionFactory connectionFactory,
            UniProtKBMessageListener uniProtKBMessageListener,
            UniProtKBAsyncDownloadQueueConfigProperties configProps) {
        return getSimpleMessageListenerContainer(
                connectionFactory, uniProtKBMessageListener, configProps);
    }

    @Bean
    public MessageListenerContainer uniRefMessageListenerContainer(
            ConnectionFactory connectionFactory,
            UniRefMessageListener uniRefMessageListener,
            UniRefAsyncDownloadQueueConfigProperties configProps) {

        return getSimpleMessageListenerContainer(
                connectionFactory, uniRefMessageListener, configProps);
    }

    private static SimpleMessageListenerContainer getSimpleMessageListenerContainer(
            ConnectionFactory connectionFactory,
            MessageListener messageListener,
            AsyncDownloadQueueConfigProperties configProps) {
        SimpleMessageListenerContainer simpleMessageListenerContainer =
                new SimpleMessageListenerContainer();
        simpleMessageListenerContainer.setConnectionFactory(connectionFactory);
        simpleMessageListenerContainer.setQueueNames(configProps.getQueueName());
        simpleMessageListenerContainer.setMessageListener(messageListener);
        simpleMessageListenerContainer.setConcurrentConsumers(configProps.getConcurrentConsumers());
        simpleMessageListenerContainer.setDefaultRequeueRejected(false);
        simpleMessageListenerContainer.setPrefetchCount(configProps.getPrefetchCount());
        return simpleMessageListenerContainer;
    }
}
