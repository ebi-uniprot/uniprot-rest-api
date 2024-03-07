package org.uniprot.api.async.download.messaging.config.uniprotkb;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Repository;

@Repository
public class UniProtKBRabbitTemplate extends RabbitTemplate {

    public UniProtKBRabbitTemplate(
            ConnectionFactory connectionFactory,
            UniProtKBAsyncDownloadQueueConfigProperties uniProtKBAsyncDownloadQueueConfigProperties,
            MessageConverter jsonMessageConverter) {
        super(connectionFactory);
        setMessageConverter(jsonMessageConverter);
        setExchange(uniProtKBAsyncDownloadQueueConfigProperties.getExchangeName());
        setRoutingKey(uniProtKBAsyncDownloadQueueConfigProperties.getRoutingKey());
    }
}
