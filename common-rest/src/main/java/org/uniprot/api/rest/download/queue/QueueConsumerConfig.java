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
public class QueueConsumerConfig {

    @Bean
    public MessageListenerContainer messageListenerContainer(
            ConnectionFactory connectionFactory,
            @Qualifier("Consumer") MessageListener messageListener,
            RabbitMQConfigProperties rabbitMQConfigProperties) {

        SimpleMessageListenerContainer simpleMessageListenerContainer =
                new SimpleMessageListenerContainer();
        simpleMessageListenerContainer.setConnectionFactory(connectionFactory);
        simpleMessageListenerContainer.setQueueNames(rabbitMQConfigProperties.getQueueName());
        simpleMessageListenerContainer.setMessageListener(messageListener);
        simpleMessageListenerContainer.setConcurrentConsumers(
                rabbitMQConfigProperties.getConcurrentConsumers());
        simpleMessageListenerContainer.setDefaultRequeueRejected(false);
        return simpleMessageListenerContainer;
    }
}
