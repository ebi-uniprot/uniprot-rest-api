package org.uniprot.api.async.download.messaging.config.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Rabbit MQ properties bean that will be injected with values from application.properties.
 *
 * @author sahmad
 */
@Data
@ConfigurationProperties(prefix = "spring.amqp.rabbit")
public class RabbitMQConnectionConfig {
    private String host;

    private String user;

    private String password;

    private int port;
}
