package org.uniprot.api.uniprotkb.common.repository.store.protlm;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "voldemort.google.protlm")
@Data
public class ProtLMStoreConfigProperties {
    private String host;
    private int numberOfConnections;
    private String storeName;
    private boolean brotliEnabled;
}
