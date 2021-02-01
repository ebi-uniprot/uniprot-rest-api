package org.uniprot.api.uniparc.repository.store;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamerConfigProperties;

/**
 * @author sahmad
 * @created 09/12/2020
 */
public class UniParcStoreConfigTest {
    @Test
    void testRestTemplate() {
        UniParcStoreConfig uniParcStoreConfig = new UniParcStoreConfig();
        RDFStreamerConfigProperties properties = new RDFStreamerConfigProperties();
        properties.setRequestUrl("http://localhost");
        properties.setBatchSize(25);
        properties.setMaxRetries(2);
        properties.setRetryDelayMillis(100);
        RestTemplate template = uniParcStoreConfig.restTemplate(properties);
        Assertions.assertNotNull(template);
    }
}
