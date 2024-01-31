package org.uniprot.api.uniprotkb.common.repository.store;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@ConfigurationProperties(prefix = "voldemort.uniprot")
@Data
public class UniProtStoreConfigProperties {
    private String host;
    private int numberOfConnections;
    private String storeName;

    private boolean brotliEnabled;
}
