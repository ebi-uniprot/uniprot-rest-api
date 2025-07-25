package org.uniprot.api.uniprotkb.common.repository.store.protnlm;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "voldemort.google.protnlm")
@Data
public class ProtNLMStoreConfigProperties {
    private String host;
    private int numberOfConnections;
    private String storeName;
    private boolean brotliEnabled;
}
