package org.uniprot.api.uniref.repository.store;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author lgonzales
 * @since 09/07/2020
 */
@ConfigurationProperties(prefix = "voldemort.uniref.light")
@Data
public class UniRefLightStoreConfigProperties {
    private String host;
    private String storeName;
    private int numberOfConnections;
    private int fetchMaxRetries;
    private int fetchRetryDelayMillis;
}
