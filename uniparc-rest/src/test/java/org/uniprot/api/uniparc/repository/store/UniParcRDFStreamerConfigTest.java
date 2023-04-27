package org.uniprot.api.uniparc.repository.store;

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

class UniParcRDFStreamerConfigTest {
    @Mock
    private PrologProvider prologProvider;
    @Mock
    private TagPositionProvider tagPositionProvider;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private RDFServiceFactory rdfServiceFactory;
    private RDFStreamerConfigProperties properties;
    private UniParcRDFStreamerConfig uniparcRDFStreamerConfig;

    @BeforeEach
    void setUp() {
        uniparcRDFStreamerConfig = new UniParcRDFStreamerConfig(prologProvider, tagPositionProvider);
        properties = new RDFStreamerConfigProperties();
        properties.setRequestUrl("http://localhost");
        properties.setBatchSize(25);
        properties.setMaxRetries(2);
        properties.setRetryDelayMillis(100);
    }

    @Test
    void uniparcRdfStreamer() {
        RDFStreamer rdfStreamer = uniparcRDFStreamerConfig.uniparcRdfStreamer(properties, rdfServiceFactory);
        assertNotNull(rdfStreamer);
    }

    @Test
    void uniparcRdfServiceFactory() {
        RDFServiceFactory rdfServiceFactory = uniparcRDFStreamerConfig.uniparcRdfServiceFactory(restTemplate);
        assertNotNull(rdfServiceFactory);
    }

    @Test
    void uniparcRdfRestTemplate() {
        RestTemplate template = uniparcRDFStreamerConfig.uniparcRdfRestTemplate(properties);
        assertNotNull(template);
    }
}