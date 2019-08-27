package org.uniprot.api.uniref.repository.store;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 *
 * @author jluo
 * @date: 20 Aug 2019
 *
*/
@ConfigurationProperties(prefix = "voldemort.uniprot")
@Data
public class UniRefStoreConfigProperties {
    private String host;
    private int numberOfConnections;
    private String storeName;
}

