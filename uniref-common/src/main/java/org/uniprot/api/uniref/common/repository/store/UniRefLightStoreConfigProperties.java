package org.uniprot.api.uniref.common.repository.store;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

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

    private boolean brotliEnabled;
}
