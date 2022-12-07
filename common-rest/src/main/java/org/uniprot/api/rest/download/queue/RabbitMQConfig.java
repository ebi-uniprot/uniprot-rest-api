package org.uniprot.api.rest.download.queue;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Initialisation code for Rabbit MQ to be used by both producer and consumer
 *
 * @author sahmad
 * @created 23/11/2022
 */
@Configuration
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
    public Exchange downloadExchange(RabbitMQConfigProperties rabbitMQConfigProperties) {
        return ExchangeBuilder.directExchange(rabbitMQConfigProperties.getExchangeName())
                .durable(rabbitMQConfigProperties.isDurable())
                .build();
    }

    @Bean
    public Queue downloadQueue(RabbitMQConfigProperties rabbitMQConfigProperties) {
        if (rabbitMQConfigProperties.isDurable()) {
            return QueueBuilder.durable(rabbitMQConfigProperties.getQueueName()).build();
        } else {
            return QueueBuilder.nonDurable(rabbitMQConfigProperties.getQueueName()).build();
        }
    }

    @Bean
    public Binding downloadBinding(
            Queue downloadQueue,
            Exchange downloadExchange,
            RabbitMQConfigProperties rabbitMQConfigProperties) {
        return BindingBuilder.bind(downloadQueue)
                .to((DirectExchange) downloadExchange)
                .with(rabbitMQConfigProperties.getRoutingKey());
    }

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        return rabbitAdmin;
    }
}
