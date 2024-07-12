package org.uniprot.api.uniparc.common.repository.store.crossref;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "voldemort.cross.reference")
public class UniParcCrossReferenceStoreConfigProperties {
    private String host;
    private int numberOfConnections;
    private String storeName;

    private boolean brotliEnabled;
    private int fetchMaxRetries;
    private int fetchRetryDelayMillis;
    private int batchSize;
}
