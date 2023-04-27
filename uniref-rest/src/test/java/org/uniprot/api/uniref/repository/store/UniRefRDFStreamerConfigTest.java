package org.uniprot.api.uniref.repository.store;

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

class UniRefRDFStreamerConfigTest {
    @Mock
    private PrologProvider prologProvider;
    @Mock
    private TagPositionProvider tagPositionProvider;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private RDFServiceFactory rdfServiceFactory;
    private RDFStreamerConfigProperties properties;
    private UniRefRDFStreamerConfig unirefRDFStreamerConfig;

    @BeforeEach
    void setUp() {
        unirefRDFStreamerConfig = new UniRefRDFStreamerConfig(prologProvider, tagPositionProvider);
        properties = new RDFStreamerConfigProperties();
        properties.setRequestUrl("http://localhost");
        properties.setBatchSize(25);
        properties.setMaxRetries(2);
        properties.setRetryDelayMillis(100);
    }

    @Test
    void unirefRdfStreamer() {
        RDFStreamer rdfStreamer = unirefRDFStreamerConfig.unirefRdfStreamer(properties, rdfServiceFactory);
        assertNotNull(rdfStreamer);
    }

    @Test
    void unirefRdfServiceFactory() {
        RDFServiceFactory rdfServiceFactory = unirefRDFStreamerConfig.unirefRdfServiceFactory(restTemplate);
        assertNotNull(rdfServiceFactory);
    }

    @Test
    void unirefRdfRestTemplate() {
        RestTemplate template = unirefRDFStreamerConfig.unirefRdfRestTemplate(properties);
        assertNotNull(template);
    }
}