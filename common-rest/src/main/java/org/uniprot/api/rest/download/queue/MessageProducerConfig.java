package org.uniprot.api.rest.download.queue;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author sahmad
 * @created 22/11/2022
 */
@Configuration
public class MessageProducerConfig {

    @Bean
    @Profile("live")
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            RabbitMQConfigProperties rabbitMQConfigProperties,
            MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setExchange(rabbitMQConfigProperties.getExchangeName());
        template.setRoutingKey(rabbitMQConfigProperties.getRoutingKey());
        return template;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setCreateMessageIds(true);
        return converter;
    }
}
