package org.uniprot.api.common.repository.stream.store;

import lombok.Data;

/**
 * @author lgonzales
 * @since 25/02/2021
 */
@Data
public class StoreConfigProperties {
    private String host;
    private int numberOfConnections;
    private String storeName;

    private boolean brotliEnabled;
}
