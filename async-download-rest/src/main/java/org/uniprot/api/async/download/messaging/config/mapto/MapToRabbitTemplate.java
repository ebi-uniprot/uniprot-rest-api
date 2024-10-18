package org.uniprot.api.async.download.messaging.config.mapto;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Repository;

@Repository
public class MapToRabbitTemplate extends RabbitTemplate {
    public MapToRabbitTemplate(
            ConnectionFactory connectionFactory,
            MapToAsyncDownloadQueueConfigProperties mapToAsyncDownloadQueueConfigProperties,
            MessageConverter jsonMessageConverter) {
        super(connectionFactory);
        setMessageConverter(jsonMessageConverter);
        setExchange(mapToAsyncDownloadQueueConfigProperties.getExchangeName());
        setRoutingKey(mapToAsyncDownloadQueueConfigProperties.getRoutingKey());
    }
}
