package org.uniprot.api.rest.message;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;

public class MessageConfigTest {

    @Bean
    public RabbitTemplate rabbitTemplate(){
        String queueName = "teste";
        ConnectionFactory connectionFactory = new CachingConnectionFactory();
        AmqpAdmin admin = new RabbitAdmin(connectionFactory);
        admin.declareQueue(new Queue(queueName));
        return new RabbitTemplate(connectionFactory);
    }

/*    @Bean
    public RabbitAdmin rabbitAdmin() {
        return new RabbitAdmin();
    }*/

//    @Bean
//    public ConnectionFactory connectionFactory() {
//        com.rabbitmq.client.ConnectionFactory connectionFactory = new com.rabbitmq.client.ConnectionFactory();
//        connectionFactory.setConnectionTimeout(1);
////        connectionFactory.setHost(this.rabbitHost);
////        connectionFactory.setPort(this.rabbitPort);
//        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(
//                connectionFactory);
//        return cachingConnectionFactory;
//    }

//    @Bean
//    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
//            SimpleRabbitListenerContainerFactoryConfigurer containerFactoryConfigurer,
//            ConnectionFactory connectionFactory) {
//
//        SimpleRabbitListenerContainerFactory listenerContainerFactory =
//                new SimpleRabbitListenerContainerFactory();
//        containerFactoryConfigurer.configure(listenerContainerFactory, connectionFactory);
//
//        return listenerContainerFactory;
//    }

}
