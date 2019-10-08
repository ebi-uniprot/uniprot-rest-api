package org.uniprot.api.uniprotkb.repository.store;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.uniprot.api.common.repository.store.StoreStreamer;

/**
 * This class represents configurable properties of {@link StoreStreamer} instances.
 *
 * <p>Created 22/08/18
 *
 * @author Edd
 */
@ConfigurationProperties(prefix = "streamer")
@Data
public class StreamerConfigProperties {
    private StreamerProperties uniprot;

    @Data
    static class StreamerProperties {
        private int searchBatchSize;
        private int storeBatchSize;
        private int storeFetchMaxRetries;
        private int storeFetchRetryDelayMillis;
    }
}
