package org.uniprot.api.support.data.subcellular.response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamerConfigProperties;
import org.uniprot.api.support.data.literature.response.LiteratureStreamConfig;

/**
 * @author sahmad
 * @created 02/02/2021
 */
class SubcellularLocationStreamConfigTest {

    @Test
    void testRestTemplate() {
        SubcellularLocationStreamConfig streamConfig = new SubcellularLocationStreamConfig();
        RDFStreamerConfigProperties properties = new RDFStreamerConfigProperties();
        properties.setRequestUrl("http://localhost");
        properties.setBatchSize(25);
        properties.setMaxRetries(2);
        properties.setRetryDelayMillis(100);
        RestTemplate template = streamConfig.locationRDFRestTemplate(properties);
        Assertions.assertNotNull(template);
    }
}
