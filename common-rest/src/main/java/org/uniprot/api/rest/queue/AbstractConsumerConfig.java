package org.uniprot.api.rest.queue;

import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author sahmad
 * @created 22/11/2022
 */
@Configuration
public abstract class AbstractConsumerConfig {
    protected abstract void setQueueName(SimpleMessageListenerContainer messageListenerContainer);

    @Bean
    public MessageListenerContainer messageListenerContainer(ConnectionFactory connectionFactory,
                                                                   @Qualifier("Consumer") MessageListener messageListener) {

        SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
        simpleMessageListenerContainer.setConnectionFactory(connectionFactory);
        setQueueName(simpleMessageListenerContainer);
        simpleMessageListenerContainer.setMessageListener(messageListener);
        return simpleMessageListenerContainer;
    }
}
