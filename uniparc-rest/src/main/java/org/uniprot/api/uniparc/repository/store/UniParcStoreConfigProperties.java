package org.uniprot.api.uniparc.repository.store;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
}
