package org.uniprot.api.rest.download;



import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@TestConfiguration
public class MessageQueueTestConfig {

    @Bean
    @Profile("offline")
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory){
        return new RabbitTemplate(connectionFactory);
    }

    @Bean
    @Profile("offline")
    public ConnectionFactory connectionFactory() {
        ConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
        return cachingConnectionFactory;
    }

    @Bean
    @Profile("offline")
    public RabbitProperties rabbitProperties() {
        RabbitProperties props = new RabbitProperties();
        props.setPort(6672);
        return props;
    }
}
