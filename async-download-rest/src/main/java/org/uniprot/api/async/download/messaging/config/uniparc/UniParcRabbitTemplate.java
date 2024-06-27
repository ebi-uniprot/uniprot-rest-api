package org.uniprot.api.async.download.messaging.config.uniparc;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Repository;

@Repository
public class UniParcRabbitTemplate extends RabbitTemplate {
    public UniParcRabbitTemplate(
            ConnectionFactory connectionFactory,
            UniParcAsyncDownloadQueueConfigProperties uniParcAsyncDownloadQueueConfigProperties,
            MessageConverter jsonMessageConverter) {
        super(connectionFactory);
        setMessageConverter(jsonMessageConverter);
        setExchange(uniParcAsyncDownloadQueueConfigProperties.getExchangeName());
        setRoutingKey(uniParcAsyncDownloadQueueConfigProperties.getRoutingKey());
    }
}
