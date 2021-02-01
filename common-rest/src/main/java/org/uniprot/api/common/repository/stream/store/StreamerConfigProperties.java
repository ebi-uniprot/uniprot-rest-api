package org.uniprot.api.common.repository.stream.store;

import lombok.Data;

/**
 * This class represents configurable properties of {@link StoreStreamer} instances.
 *
 * <p>Created 22/08/18
 *
 * @author Edd
 */
@Data
public class StreamerConfigProperties {
    private int storeBatchSize;
    private int storeFetchMaxRetries;
    private int storeFetchRetryDelayMillis;
    private int storeMaxCountToRetrieve;

    private String zkHost;
    private String idFieldName;
    private String requestHandler;
    private String collection;
}
