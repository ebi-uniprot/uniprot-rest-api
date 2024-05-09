package org.uniprot.api.async.download.messaging.config.idmapping;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Repository;

@Repository
public class IdMappingRabbitTemplate extends RabbitTemplate {

    public IdMappingRabbitTemplate(
            ConnectionFactory connectionFactory,
            IdMappingAsyncDownloadQueueConfigProperties idMappingAsyncDownloadQueueConfigProperties,
            MessageConverter jsonMessageConverter) {
        super(connectionFactory);
        setMessageConverter(jsonMessageConverter);
        setExchange(idMappingAsyncDownloadQueueConfigProperties.getExchangeName());
        setRoutingKey(idMappingAsyncDownloadQueueConfigProperties.getRoutingKey());
    }
}
