package org.uniprot.api.rest.download.queue;

import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author sahmad
 * @created 22/11/2022
 */
@Configuration
@Profile({"asyncDownload"})
public class QueueConsumerConfig {

    @Bean
    public MessageListenerContainer idMappingMessageListenerContainer(
            ConnectionFactory connectionFactory,
            @Qualifier("IdMappingDownloadListener") MessageListener messageListener,
            IdMappingAsyncDownloadQueueConfigProperties configProps) {

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

    @Bean
    public MessageListenerContainer uniProtKBMessageListenerContainer(
            ConnectionFactory connectionFactory,
            @Qualifier("UniProtKBDownloadListener") MessageListener messageListener,
            UniProtKBAsyncDownloadQueueConfigProperties configProps) {

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

    @Bean
    public MessageListenerContainer uniRefMessageListenerContainer(
            ConnectionFactory connectionFactory,
            @Qualifier("UniRefDownloadListener") MessageListener messageListener,
            UniRefAsyncDownloadQueueConfigProperties configProps) {

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
