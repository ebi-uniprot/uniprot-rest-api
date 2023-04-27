package org.uniprot.api.support.data.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.stream.rdf.PrologProvider;
import org.uniprot.api.common.repository.stream.rdf.RDFServiceFactory;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamerConfigProperties;
import org.uniprot.api.rest.service.TagPositionProvider;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SupportDataRDFStreamerConfigTest {
    @Mock
    private PrologProvider prologProvider;
    @Mock
    private TagPositionProvider tagPositionProvider;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private RDFServiceFactory rdfServiceFactory;
    private RDFStreamerConfigProperties properties;
    private SupportDataRDFStreamerConfig supportDataRDFStreamerConfig;

    @BeforeEach
    void setUp() {
        supportDataRDFStreamerConfig = new SupportDataRDFStreamerConfig(prologProvider, tagPositionProvider);
        properties = new RDFStreamerConfigProperties();
        properties.setRequestUrl("http://localhost");
        properties.setBatchSize(25);
        properties.setMaxRetries(2);
        properties.setRetryDelayMillis(100);
    }

    @Test
    void supportDataRdfStreamer() {
        RDFStreamer rdfStreamer = supportDataRDFStreamerConfig.supportDataRdfStreamer(properties, rdfServiceFactory);
        assertNotNull(rdfStreamer);
    }

    @Test
    void supportDataRdfServiceFactory() {
        RDFServiceFactory rdfServiceFactory = supportDataRDFStreamerConfig.supportDataRdfServiceFactory(restTemplate);
        assertNotNull(rdfServiceFactory);
    }

    @Test
    void supportDataRdfRestTemplate() {
        RestTemplate template = supportDataRDFStreamerConfig.supportDataRdfRestTemplate(properties);
        assertNotNull(template);
    }
}