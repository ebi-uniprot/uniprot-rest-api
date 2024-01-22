package org.uniprot.api.uniprotkb.repository.store;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.stream.rdf.*;
import org.uniprot.api.rest.service.TagPositionProvider;

class UniProtRdfStreamerConfigTest {
    @Mock private PrologProvider prologProvider;
    @Mock private TagPositionProvider tagPositionProvider;
    @Mock private RestTemplate restTemplate;
    @Mock private RdfServiceFactory rdfServiceFactory;
    @Mock private RdfEntryCountProvider rdfEntryCountProvider;
    private RdfStreamerConfigProperties properties;
    private UniProtRdfStreamerConfig uniprotRdfStreamerConfig;

    @BeforeEach
    void setUp() {
        uniprotRdfStreamerConfig =
                new UniProtRdfStreamerConfig(
                        prologProvider, tagPositionProvider, rdfEntryCountProvider);
        properties = new RdfStreamerConfigProperties();
        properties.setRequestUrl("http://localhost");
        properties.setBatchSize(25);
        properties.setMaxRetries(2);
        properties.setRetryDelayMillis(100);
    }

    @Test
    void uniProtRdfStreamer() {
        RdfStreamer rdfStreamer =
                uniprotRdfStreamerConfig.uniProtRdfStreamer(properties, rdfServiceFactory);
        assertNotNull(rdfStreamer);
    }

    @Test
    void uniProtRdfServiceFactory() {
        RdfServiceFactory rdfServiceFactory =
                uniprotRdfStreamerConfig.uniProtRdfServiceFactory(restTemplate);
        assertNotNull(rdfServiceFactory);
    }

    @Test
    void uniProtRdfRestTemplate() {
        RestTemplate template = uniprotRdfStreamerConfig.uniProtRdfRestTemplate(properties);
        assertNotNull(template);
    }
}
