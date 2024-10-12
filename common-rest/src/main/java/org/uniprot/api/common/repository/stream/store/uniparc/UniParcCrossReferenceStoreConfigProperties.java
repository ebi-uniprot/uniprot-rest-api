package org.uniprot.api.common.repository.stream.store.uniparc;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "voldemort.uniparc.cross.reference")
public class UniParcCrossReferenceStoreConfigProperties {
    private String host;
    private int numberOfConnections;
    private String storeName;

    private boolean brotliEnabled;
    private int fetchMaxRetries;
    private int fetchRetryDelayMillis;
    private int groupSize;
}
