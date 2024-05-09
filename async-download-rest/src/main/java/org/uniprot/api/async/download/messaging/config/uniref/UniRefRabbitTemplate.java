package org.uniprot.api.async.download.messaging.config.uniref;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Repository;

@Repository
public class UniRefRabbitTemplate extends RabbitTemplate {
    public UniRefRabbitTemplate(
            ConnectionFactory connectionFactory,
            UniRefAsyncDownloadQueueConfigProperties uniRefAsyncDownloadQueueConfigProperties,
            MessageConverter jsonMessageConverter) {
        super(connectionFactory);
        setMessageConverter(jsonMessageConverter);
        setExchange(uniRefAsyncDownloadQueueConfigProperties.getExchangeName());
        setRoutingKey(uniRefAsyncDownloadQueueConfigProperties.getRoutingKey());
    }
}
