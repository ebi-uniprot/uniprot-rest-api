package org.uniprot.api.support.data.disease.response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamerConfigProperties;

/**
 * @author sahmad
 * @created 02/02/2021
 */
class DiseaseStreamConfigTest {

    @Test
    void testRestTemplate() {
        DiseaseStreamConfig streamConfig = new DiseaseStreamConfig();
        RDFStreamerConfigProperties properties = new RDFStreamerConfigProperties();
        properties.setRequestUrl("http://localhost");
        properties.setBatchSize(25);
        properties.setMaxRetries(2);
        properties.setRetryDelayMillis(100);
        RestTemplate template = streamConfig.diseaseRestTemplate(properties);
        Assertions.assertNotNull(template);
    }
}
