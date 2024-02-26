package org.uniprot.api.async.download.queue.common;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Initialisation code for Rabbit MQ to be used by both producer and consumer
 *
 * @author sahmad
 * @created 23/11/2022
 */
@Configuration
@Profile({"asyncDownload"})
public class RabbitMQConfig {

    @Bean
    public RabbitMQConfigProperties rabbitMQConfigProperties() {
        return new RabbitMQConfigProperties();
    }

    @Bean
    public ConnectionFactory connectionFactory(RabbitMQConfigProperties rabbitMQConfigProperties) {
        CachingConnectionFactory connectionFactory =
                new CachingConnectionFactory(rabbitMQConfigProperties.getHost());
        connectionFactory.setUsername(rabbitMQConfigProperties.getUser());
        connectionFactory.setPassword(rabbitMQConfigProperties.getPassword());
        connectionFactory.setPort(rabbitMQConfigProperties.getPort());
        return connectionFactory;
    }

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        return rabbitAdmin;
    }
}
