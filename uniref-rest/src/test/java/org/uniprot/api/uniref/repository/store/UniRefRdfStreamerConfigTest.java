package org.uniprot.api.uniref.repository.store;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.stream.rdf.PrologProvider;
import org.uniprot.api.common.repository.stream.rdf.RdfServiceFactory;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamerConfigProperties;
import org.uniprot.api.rest.service.TagPositionProvider;

class UniRefRdfStreamerConfigTest {
    @Mock private PrologProvider prologProvider;
    @Mock private TagPositionProvider tagPositionProvider;
    @Mock private RestTemplate restTemplate;
    @Mock private RdfServiceFactory rdfServiceFactory;
    private RdfStreamerConfigProperties properties;
    private UniRefRdfStreamerConfig unirefRdfStreamerConfig;

    @BeforeEach
    void setUp() {
        unirefRdfStreamerConfig = new UniRefRdfStreamerConfig(prologProvider, tagPositionProvider);
        properties = new RdfStreamerConfigProperties();
        properties.setRequestUrl("http://localhost");
        properties.setBatchSize(25);
        properties.setMaxRetries(2);
        properties.setRetryDelayMillis(100);
    }

    @Test
    void unirefRdfStreamer() {
        RdfStreamer rdfStreamer =
                unirefRdfStreamerConfig.unirefRdfStreamer(properties, rdfServiceFactory);
        assertNotNull(rdfStreamer);
    }

    @Test
    void unirefRdfServiceFactory() {
        RdfServiceFactory rdfServiceFactory =
                unirefRdfStreamerConfig.unirefRdfServiceFactory(restTemplate);
        assertNotNull(rdfServiceFactory);
    }

    @Test
    void unirefRdfRestTemplate() {
        RestTemplate template = unirefRdfStreamerConfig.unirefRdfRestTemplate(properties);
        assertNotNull(template);
    }
}
