package org.uniprot.api.rest.download;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.rest.download.message.EmbeddedInMemoryQpidBroker;
import org.uniprot.api.rest.download.queue.RabbitMQConfigProperties;

@TestConfiguration
public class MessageQueueTestConfig {

    @Bean
    @Profile("offline")
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter,
                                         RabbitMQConfigProperties rabbitMQConfigProperties) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        rabbitTemplate.setExchange(rabbitMQConfigProperties.getExchangeName());
        rabbitTemplate.setRoutingKey(rabbitMQConfigProperties.getRoutingKey());
        return rabbitTemplate;
    }

    @Bean
    @Profile("offline")
    public ConnectionFactory connectionFactory(RabbitMQConfigProperties rabbitMQConfigProperties) {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(rabbitMQConfigProperties.getHost());
        connectionFactory.setPort(rabbitMQConfigProperties.getPort());
        connectionFactory.setUsername(rabbitMQConfigProperties.getUser());
        connectionFactory.setPassword(rabbitMQConfigProperties.getPassword());
        return connectionFactory;
    }


}
