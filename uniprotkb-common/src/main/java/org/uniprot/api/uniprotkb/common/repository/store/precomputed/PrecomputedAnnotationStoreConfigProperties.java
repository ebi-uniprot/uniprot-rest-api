package org.uniprot.api.uniprotkb.common.repository.store.precomputed;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "voldemort.precomputed.annotation")
@Data
public class PrecomputedAnnotationStoreConfigProperties {
    private String host;
    private int numberOfConnections;
    private String storeName;
    private boolean brotliEnabled;
}
