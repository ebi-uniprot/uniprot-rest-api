package org.uniprot.api.async.download.messaging.config.map;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Repository;

@Repository
public class MapRabbitTemplate extends RabbitTemplate {
    public MapRabbitTemplate(
            ConnectionFactory connectionFactory,
            MapAsyncDownloadQueueConfigProperties mapAsyncDownloadQueueConfigProperties,
            MessageConverter jsonMessageConverter) {
        super(connectionFactory);
        setMessageConverter(jsonMessageConverter);
        setExchange(mapAsyncDownloadQueueConfigProperties.getExchangeName());
        setRoutingKey(mapAsyncDownloadQueueConfigProperties.getRoutingKey());
    }
}
