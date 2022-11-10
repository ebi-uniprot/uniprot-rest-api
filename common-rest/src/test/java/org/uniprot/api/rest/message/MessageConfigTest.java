package org.uniprot.api.rest.message;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.amqp.AbstractRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;

public class MessageConfigTest {

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        String queueName = "teste";
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        Queue queue = new Queue(queueName);
        admin.declareQueue(queue);
        DirectExchange exchange = new DirectExchange("direct-exchange");
        admin.declareExchange(exchange);
        admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("routing-key-teste"));
        return new RabbitTemplate(connectionFactory);
    }

    //    @Bean
    //    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
    //        String queueName = "teste";
    //        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
    //        admin.declareQueue(new Queue(queueName));
    //        return admin;
    //    }

    @Bean
    public ConnectionFactory connectionFactory() {
        //        com.rabbitmq.client.ConnectionFactory connectionFactory = new
        // com.rabbitmq.client.ConnectionFactory();
        //        connectionFactory.setConnectionTimeout(1);
        //        connectionFactory.setHost("localhost");
        //        connectionFactory.setPort(5672);
        ConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
        return cachingConnectionFactory;
    }

    @Bean
    public RabbitProperties rabbitProperties() {
        return new RabbitProperties();
    }

    @Bean
    AbstractRabbitListenerContainerFactoryConfigurer containerFactoryConfigurer(
            SimpleRabbitListenerContainerFactory listenerContainerFactory,
            ConnectionFactory connectionFactory,
            RabbitProperties rabbitProperties) {
        MySimpleRabbitListenerContainerFactoryConfigurer configurer =
                new MySimpleRabbitListenerContainerFactoryConfigurer();
        configurer.setRabbitProperties(rabbitProperties);
        configurer.configure(listenerContainerFactory, connectionFactory);
        return configurer;
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory() {
        return new SimpleRabbitListenerContainerFactory();
    }

    private static class MySimpleRabbitListenerContainerFactoryConfigurer
            extends AbstractRabbitListenerContainerFactoryConfigurer<
                    SimpleRabbitListenerContainerFactory> {

        @Override
        public void configure(
                SimpleRabbitListenerContainerFactory factory, ConnectionFactory connectionFactory) {
            PropertyMapper map = PropertyMapper.get();
            RabbitProperties.SimpleContainer config =
                    getRabbitProperties().getListener().getSimple();
            configure(factory, connectionFactory, config);
            map.from(config::getConcurrency).whenNonNull().to(factory::setConcurrentConsumers);
            map.from(config::getMaxConcurrency)
                    .whenNonNull()
                    .to(factory::setMaxConcurrentConsumers);
            map.from(config::getBatchSize).whenNonNull().to(factory::setBatchSize);
        }

        @Override
        protected void setRabbitProperties(RabbitProperties rabbitProperties) {
            super.setRabbitProperties(rabbitProperties);
        }
    }
}
