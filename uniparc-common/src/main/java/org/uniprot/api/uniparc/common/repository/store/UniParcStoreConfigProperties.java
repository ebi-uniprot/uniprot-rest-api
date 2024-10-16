package org.uniprot.api.uniparc.common.repository.store;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * @author lgonzales
 * @since 2020-03-04
 */
@ConfigurationProperties(prefix = "voldemort.uniparc")
@Data
public class UniParcStoreConfigProperties {
    private String host;
    private int numberOfConnections;
    private String storeName;

    private boolean brotliEnabled;
}
