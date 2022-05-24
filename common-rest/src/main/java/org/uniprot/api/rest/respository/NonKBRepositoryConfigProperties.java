package org.uniprot.api.rest.respository;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Solr properties bean that will be injected with values from application.properties.
 *
 * @author lgonzales
 */
@Data
@ConfigurationProperties(prefix = "spring.data.solr.nonkb")
public class NonKBRepositoryConfigProperties {
    private static final int DEFAULT_CONNECTION_TIMEOUT_MILLIS = 1000 * 20;
    private static final int DEFAULT_SOCKET_TIMEOUT_MILLIS = 1000 * 60 * 60;

    private String zkHost;

    private String httphost;

    private String username;

    private String password;

    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT_MILLIS;

    private int socketTimeout = DEFAULT_SOCKET_TIMEOUT_MILLIS;
}
