package org.uniprot.api.async.download.messaging.config.common;

import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

/**
 * @author sahmad
 * @created 22/11/2022
 */
public class QueueConsumerConfigUtils {

    public static SimpleMessageListenerContainer getSimpleMessageListenerContainer(
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
