package org.uniprot.api.uniparc.common.repository.rdf;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.stream.rdf.*;
import org.uniprot.api.rest.service.TagPositionProvider;

class UniParcRdfStreamerConfigTest {
    @Mock private PrologProvider prologProvider;
    @Mock private TagPositionProvider tagPositionProvider;
    @Mock private RestTemplate restTemplate;
    @Mock private RdfServiceFactory rdfServiceFactory;
    @Mock private RdfEntryCountProvider rdfEntryCountProvider;
    private RdfStreamerConfigProperties properties;
    private UniParcRdfStreamerConfig uniparcRdfStreamerConfig;

    @BeforeEach
    void setUp() {
        uniparcRdfStreamerConfig =
                new UniParcRdfStreamerConfig(
                        prologProvider, tagPositionProvider, rdfEntryCountProvider);
        properties = new RdfStreamerConfigProperties();
        properties.setRequestUrl("http://localhost");
        properties.setBatchSize(25);
        properties.setMaxRetries(2);
        properties.setRetryDelayMillis(100);
    }

    @Test
    void uniparcRdfStreamer() {
        RdfStreamer rdfStreamer =
                uniparcRdfStreamerConfig.uniparcRdfStreamer(properties, rdfServiceFactory);
        assertNotNull(rdfStreamer);
    }

    @Test
    void uniparcRdfServiceFactory() {
        RdfServiceFactory rdfServiceFactory =
                uniparcRdfStreamerConfig.uniparcRdfServiceFactory(restTemplate);
        assertNotNull(rdfServiceFactory);
    }

    @Test
    void uniparcRdfRestTemplate() {
        RestTemplate template = uniparcRdfStreamerConfig.uniparcRdfRestTemplate(properties);
        assertNotNull(template);
    }
}
