package org.uniprot.api.rest.respository;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Solr properties bean that will be injected with values from application.properties.
 *
 * @author lgonzales
 */
@Data
@ConfigurationProperties(prefix = "spring.data.solr.kb")
public class UniProtKBRepositoryConfigProperties {
    private static final int DEFAULT_CONNECTION_TIMEOUT_MILLIS = 1000 * 20;
    private static final int DEFAULT_SOCKET_TIMEOUT_MILLIS = 1000 * 60 * 60;

    private String zkHost;

    private String httphost;

    private String username;

    private String password;

    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT_MILLIS;

    private int socketTimeout = DEFAULT_SOCKET_TIMEOUT_MILLIS;
}
