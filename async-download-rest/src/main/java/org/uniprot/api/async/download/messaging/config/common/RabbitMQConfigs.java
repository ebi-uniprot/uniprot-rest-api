package org.uniprot.api.async.download.messaging.config.common;

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
public class RabbitMQConfigs {

    @Bean
    public RabbitMQConnectionConfig rabbitMQConfigProperties() {
        return new RabbitMQConnectionConfig();
    }

    @Bean
    public ConnectionFactory connectionFactory(RabbitMQConnectionConfig rabbitMQConnectionConfig) {
        CachingConnectionFactory connectionFactory =
                new CachingConnectionFactory(rabbitMQConnectionConfig.getHost());
        connectionFactory.setUsername(rabbitMQConnectionConfig.getUser());
        connectionFactory.setPassword(rabbitMQConnectionConfig.getPassword());
        connectionFactory.setPort(rabbitMQConnectionConfig.getPort());
        return connectionFactory;
    }

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        return rabbitAdmin;
    }
}
