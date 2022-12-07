package org.uniprot.api.rest.download.queue;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rabbit MQ properties bean that will be injected with values from application.properties.
 *
 * @author sahmad
 */
@Data
@ConfigurationProperties(prefix = "spring.amqp.rabbit")
public class RabbitMQConfigProperties {
    private String host;

    private String user;

    private String password;

    private int port;

    private String exchangeName;

    private String queueName;

    private String routingKey;

    private boolean durable;

    private int concurrentConsumers;
}
