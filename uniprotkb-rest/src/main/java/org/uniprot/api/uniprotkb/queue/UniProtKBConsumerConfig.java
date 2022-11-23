package org.uniprot.api.uniprotkb.queue;

import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.rest.queue.AbstractConsumerConfig;
import org.uniprot.api.rest.queue.RabbitMQBinding;

/**
 *
 * @author sahmad
 * @created 22/11/2022
 */
@Configuration
public class UniProtKBConsumerConfig extends AbstractConsumerConfig {
    @Override
    protected void setQueueName(SimpleMessageListenerContainer messageListenerContainer) {
        messageListenerContainer.setQueueNames(RabbitMQBinding.UNIPROTKB.getQueueName());
    }
}
