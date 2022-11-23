package org.uniprot.api.rest.queue;


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
 * @author sahmad
 * @created 23/11/2022
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost");
        connectionFactory.setUsername("guest");// TODO externalise
        connectionFactory.setPassword("guest");
        connectionFactory.setPort(5672);
        return connectionFactory;
    }

    @Bean
    public Exchange uniProtKBExchange(){
        return ExchangeBuilder.directExchange(RabbitMQBinding.UNIPROTKB.getExchangeName()).durable(true).build();
    }

    @Bean
    public Queue uniProtKBStreamQueue() {
        return QueueBuilder.durable(RabbitMQBinding.UNIPROTKB.getQueueName()).build();
    }

    @Bean
    public Binding uniProtKBStreamBinding(){
        return BindingBuilder
                .bind(uniProtKBStreamQueue())
                .to((DirectExchange) uniProtKBExchange())
                .with(RabbitMQBinding.UNIPROTKB.getRoutingKey());
    }
    // TODO add similar config for uniref and uniparc

    @Bean // TODO do we need it?
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        return rabbitAdmin ;
    }
}
