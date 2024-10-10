package org.uniprot.api.uniparc.common.repository.store.light;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "voldemort.uniparc.light")
public class UniParcLightStoreConfigProperties {
    private String host;
    private int numberOfConnections;
    private String storeName;
    private boolean brotliEnabled;
}
