package org.uniprot.api.async.download.queue.common;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.async.download.queue.idmapping.IdMappingAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.queue.uniprotkb.UniProtKBAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.queue.uniref.UniRefAsyncDownloadQueueConfigProperties;

/**
 * @author sahmad
 * @created 22/11/2022
 */
@Configuration
@Profile({"asyncDownload"})
public class MessageProducerConfig {

    @Bean("uniProtKBRabbitTemplate")
    public RabbitTemplate uniProtKBRabbitTemplate(
            ConnectionFactory connectionFactory,
            UniProtKBAsyncDownloadQueueConfigProperties uniProtKBAsyncDownloadQueueConfigProperties,
            MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setExchange(uniProtKBAsyncDownloadQueueConfigProperties.getExchangeName());
        template.setRoutingKey(uniProtKBAsyncDownloadQueueConfigProperties.getRoutingKey());
        return template;
    }

    @Bean("uniRefRabbitTemplate")
    public RabbitTemplate uniRefRabbitTemplate(
            ConnectionFactory connectionFactory,
            UniRefAsyncDownloadQueueConfigProperties uniRefAsyncDownloadQueueConfigProperties,
            MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setExchange(uniRefAsyncDownloadQueueConfigProperties.getExchangeName());
        template.setRoutingKey(uniRefAsyncDownloadQueueConfigProperties.getRoutingKey());
        return template;
    }

    @Bean("idMappingRabbitTemplate")
    public RabbitTemplate idMappingRabbitTemplate(
            ConnectionFactory connectionFactory,
            IdMappingAsyncDownloadQueueConfigProperties idMappingAsyncDownloadQueueConfigProperties,
            MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setExchange(idMappingAsyncDownloadQueueConfigProperties.getExchangeName());
        template.setRoutingKey(idMappingAsyncDownloadQueueConfigProperties.getRoutingKey());
        return template;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setCreateMessageIds(true);
        return converter;
    }
}
