package org.uniprot.api.uniref.repository.store;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author sahmad
 * @created 09/12/2020
 */
public class UniRefStoreConfigTest {
    @Disabled
    @Test
    void testCreateUnirefEntryStoreStreamer() {
        UniRefStoreConfig uniRefStoreConfig = new UniRefStoreConfig();
        UniRefLightStoreConfigProperties properties = new UniRefLightStoreConfigProperties();
        properties.setHost("tcp://localhost");
        properties.setStoreName("uniref-light");
        properties.setNumberOfConnections(10);
        properties.setFetchMaxRetries(10);
        properties.setFetchRetryDelayMillis(100);
        UniRefLightStoreClient storeClient = uniRefStoreConfig.uniRefLightStoreClient(properties);
        Assertions.assertNotNull(storeClient);
    }
}
