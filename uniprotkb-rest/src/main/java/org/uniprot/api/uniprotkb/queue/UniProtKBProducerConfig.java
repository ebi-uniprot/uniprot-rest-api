package org.uniprot.api.uniprotkb.queue;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.rest.queue.AbstractProducerConfig;
import org.uniprot.api.rest.queue.RabbitMQBinding;

/**
 * @author sahmad
 * @created 22/11/2022
 */
@Configuration
public class UniProtKBProducerConfig extends AbstractProducerConfig {
    @Override
    protected void setRabbitExchange(RabbitTemplate template) {
        template.setExchange(RabbitMQBinding.UNIPROTKB.getExchangeName());
        template.setRoutingKey(RabbitMQBinding.UNIPROTKB.getRoutingKey());
    }
}
