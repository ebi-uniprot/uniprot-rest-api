package org.uniprot.api.rest.download.queue;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.rest.download.model.DownloadRequestToArrayConverter;
import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.api.rest.request.HashGenerator;

/**
 * @author sahmad
 * @created 22/11/2022
 */
@Configuration
@Profile({"asyncDownload"})
public class MessageProducerConfig {

    @Value("${async.download.hash.salt}")
    private String hashSalt;

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

    @Bean
    public HashGenerator<DownloadRequest> hashGenerator() {
        return new HashGenerator<>(new DownloadRequestToArrayConverter(), this.hashSalt);
    }
}
