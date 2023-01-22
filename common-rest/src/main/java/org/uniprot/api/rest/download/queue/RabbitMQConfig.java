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
import org.springframework.context.annotation.Profile;

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
    @Profile("live")
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

    /**
     * Relationship among downloadQueue(DQ), retryQueue(RQ) and undeliveredQueue(UQ) : Producer
     * writes message to the exchange and the consumer receives the message from DQ. If the consumer
     * processes the message successfully then the message is removed from DQ. Else the message is
     * sent to dead letter queue(RQ) of DQ. RQ has a. no consumer b. ttl of n millis c. DLQ is DQ.
     * So after passing n millis, the message of RQ is sent to its DLQ (DQ). The consumer picks the
     * message from DQ. If the max retry of the message is not reached, the message is reprocessed.
     * Else the message is sent to UQ.
     */
    @Bean
    public Queue downloadQueue(RabbitMQConfigProperties rabbitMQConfigProperties) {
        return QueueBuilder.durable(rabbitMQConfigProperties.getQueueName())
                .deadLetterExchange(rabbitMQConfigProperties.getExchangeName())
                .deadLetterRoutingKey(rabbitMQConfigProperties.getRetryQueueName())
                .quorum()
                .build();
    }

    @Bean
    Queue retryQueue(RabbitMQConfigProperties rabbitMQConfigProperties) {
        return QueueBuilder.durable(rabbitMQConfigProperties.getRetryQueueName())
                .ttl(rabbitMQConfigProperties.getRetryDelayInMillis())
                .deadLetterExchange(rabbitMQConfigProperties.getExchangeName())
                .deadLetterRoutingKey(rabbitMQConfigProperties.getRoutingKey())
                .quorum()
                .build();
    }

    // queue where failed messages after maximum retries will end up
    @Bean
    Queue undeliveredQueue(RabbitMQConfigProperties rabbitMQConfigProperties) {
        return QueueBuilder.durable(rabbitMQConfigProperties.getRejectedQueueName())
                .quorum()
                .build();
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
    Binding retryBinding(Queue retryQueue, Exchange downloadExchange) {
        return BindingBuilder.bind(retryQueue)
                .to((DirectExchange) downloadExchange)
                .with(retryQueue.getName());
    }

    @Bean
    Binding undeliveredBinding(Queue undeliveredQueue, Exchange downloadExchange) {
        return BindingBuilder.bind(undeliveredQueue)
                .to((DirectExchange) downloadExchange)
                .with(undeliveredQueue.getName());
    }

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        return rabbitAdmin;
    }
}
