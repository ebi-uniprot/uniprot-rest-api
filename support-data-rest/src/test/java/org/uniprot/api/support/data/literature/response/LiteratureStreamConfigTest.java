package org.uniprot.api.support.data.literature.response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamerConfigProperties;

/**
 * @author sahmad
 * @created 02/02/2021
 */
class LiteratureStreamConfigTest {

    @Test
    void testRestTemplate() {
        LiteratureStreamConfig streamConfig = new LiteratureStreamConfig();
        RDFStreamerConfigProperties properties = new RDFStreamerConfigProperties();
        properties.setRequestUrl("http://localhost");
        properties.setBatchSize(25);
        properties.setMaxRetries(2);
        properties.setRetryDelayMillis(100);
        RestTemplate template = streamConfig.literatureRDFRestTemplate(properties);
        Assertions.assertNotNull(template);
    }
}
