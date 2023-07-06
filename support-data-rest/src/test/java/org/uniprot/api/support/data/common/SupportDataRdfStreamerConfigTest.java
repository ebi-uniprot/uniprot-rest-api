package org.uniprot.api.support.data.common;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.stream.rdf.PrologProvider;
import org.uniprot.api.common.repository.stream.rdf.RdfServiceFactory;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamerConfigProperties;
import org.uniprot.api.rest.output.SupportDataRdfStreamerConfig;
import org.uniprot.api.rest.service.TagPositionProvider;

class SupportDataRdfStreamerConfigTest {
    @Mock private PrologProvider prologProvider;
    @Mock private TagPositionProvider tagPositionProvider;
    @Mock private RestTemplate restTemplate;
    @Mock private RdfServiceFactory rdfServiceFactory;
    private RdfStreamerConfigProperties properties;
    private SupportDataRdfStreamerConfig supportDataRdfStreamerConfig;

    @BeforeEach
    void setUp() {
        supportDataRdfStreamerConfig =
                new SupportDataRdfStreamerConfig(prologProvider, tagPositionProvider);
        properties = new RdfStreamerConfigProperties();
        properties.setRequestUrl("http://localhost");
        properties.setBatchSize(25);
        properties.setMaxRetries(2);
        properties.setRetryDelayMillis(100);
    }

    @Test
    void supportDataRdfStreamer() {
        RdfStreamer rdfStreamer =
                supportDataRdfStreamerConfig.supportDataRdfStreamer(properties, rdfServiceFactory);
        assertNotNull(rdfStreamer);
    }

    @Test
    void supportDataRdfServiceFactory() {
        RdfServiceFactory rdfServiceFactory =
                supportDataRdfStreamerConfig.supportDataRdfServiceFactory(restTemplate);
        assertNotNull(rdfServiceFactory);
    }

    @Test
    void supportDataRdfRestTemplate() {
        RestTemplate template = supportDataRdfStreamerConfig.supportDataRdfRestTemplate(properties);
        assertNotNull(template);
    }
}
