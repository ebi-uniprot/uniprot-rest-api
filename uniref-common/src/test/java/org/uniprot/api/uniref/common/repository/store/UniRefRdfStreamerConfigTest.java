package org.uniprot.api.uniref.common.repository.store;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.stream.rdf.*;
import org.uniprot.api.rest.service.TagPositionProvider;
import org.uniprot.api.uniref.common.repository.rdf.UniRefRdfStreamerConfig;

class UniRefRdfStreamerConfigTest {
    @Mock private PrologProvider prologProvider;
    @Mock private TagPositionProvider tagPositionProvider;
    @Mock private RestTemplate restTemplate;
    @Mock private RdfServiceFactory rdfServiceFactory;
    @Mock private RdfEntryCountProvider rdfEntryCountProvider;
    private RdfStreamerConfigProperties properties;
    private UniRefRdfStreamerConfig unirefRdfStreamerConfig;

    @BeforeEach
    void setUp() {
        unirefRdfStreamerConfig =
                new UniRefRdfStreamerConfig(
                        prologProvider, tagPositionProvider, rdfEntryCountProvider);
        properties = new RdfStreamerConfigProperties();
        properties.setRequestUrl("http://localhost");
        properties.setBatchSize(25);
        properties.setMaxRetries(2);
        properties.setRetryDelayMillis(100);
    }

    @Test
    void unirefRdfStreamer() {
        RdfStreamer rdfStreamer =
                unirefRdfStreamerConfig.uniRefRdfStreamer(properties, rdfServiceFactory);
        assertNotNull(rdfStreamer);
    }

    @Test
    void unirefRdfServiceFactory() {
        RdfServiceFactory rdfServiceFactory =
                unirefRdfStreamerConfig.uniRefRdfServiceFactory(restTemplate);
        assertNotNull(rdfServiceFactory);
    }

    @Test
    void unirefRdfRestTemplate() {
        RestTemplate template = unirefRdfStreamerConfig.uniRefRdfRestTemplate(properties);
        assertNotNull(template);
    }
}
