package org.uniprot.api.support.data.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.stream.rdf.PrologProvider;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamerConfigProperties;
import org.uniprot.api.rest.service.TagProvider;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SupportDataRDFStreamerConfigTest {
    @Mock
    private PrologProvider prologProvider;
    @Mock
    private TagProvider tagProvider;
    private SupportDataRDFStreamerConfig supportDataRDFStreamerConfig;
    private RDFStreamerConfigProperties properties;

    @BeforeEach
    void setUp() {
        supportDataRDFStreamerConfig = new SupportDataRDFStreamerConfig(prologProvider, tagProvider);
        properties = new RDFStreamerConfigProperties();
        properties.setRequestUrl("http://localhost");
        properties.setBatchSize(25);
        properties.setMaxRetries(2);
        properties.setRetryDelayMillis(100);
    }

    @Test
    void supportDataRdfStreamer() {
        RestTemplate restTemplate = new RestTemplate();
        RDFStreamer rdfStreamer = supportDataRDFStreamerConfig.supportDataRdfStreamer(properties, restTemplate);
        assertNotNull(rdfStreamer);
    }

    @Test
    void supportDataRdfRestTemplate() {
        RestTemplate template = supportDataRDFStreamerConfig.supportDataRdfRestTemplate(properties);
        assertNotNull(template);
    }
}