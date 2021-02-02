package org.uniprot.api.support.data.keyword.response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamerConfigProperties;
import org.uniprot.api.support.data.disease.response.DiseaseStreamConfig;

/**
 * @author sahmad
 * @created 02/02/2021
 */
class KeywordStreamConfigTest {

    @Test
    void testRestTemplate() {
        KeywordStreamConfig streamConfig = new KeywordStreamConfig();
        RDFStreamerConfigProperties properties = new RDFStreamerConfigProperties();
        properties.setRequestUrl("http://localhost");
        properties.setBatchSize(25);
        properties.setMaxRetries(2);
        properties.setRetryDelayMillis(100);
        RestTemplate template = streamConfig.keywordRDFRestTemplate(properties);
        Assertions.assertNotNull(template);
    }
}
