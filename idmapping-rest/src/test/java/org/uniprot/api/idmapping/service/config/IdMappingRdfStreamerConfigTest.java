package org.uniprot.api.idmapping.service.config;

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

class IdMappingRdfStreamerConfigTest {
    @Mock private PrologProvider prologProvider;
    @Mock private TagPositionProvider tagPositionProvider;
    @Mock private RestTemplate restTemplate;
    @Mock private RdfServiceFactory rdfServiceFactory;
    private RdfStreamerConfigProperties properties;
    private IdMappingRdfStreamerConfig idMappingRdfStreamerConfig;

    @BeforeEach
    void setUp() {
        idMappingRdfStreamerConfig =
                new IdMappingRdfStreamerConfig(prologProvider, tagPositionProvider);
        properties = new RdfStreamerConfigProperties();
        properties.setRequestUrl("http://localhost");
        properties.setBatchSize(25);
        properties.setMaxRetries(2);
        properties.setRetryDelayMillis(100);
    }

    @Test
    void supportDataRdfServiceFactory() {
        RdfServiceFactory rdfServiceFactory =
                idMappingRdfStreamerConfig.idMappingRdfServiceFactory(restTemplate);
        assertNotNull(rdfServiceFactory);
    }

    @Test
    void idMappingRdfStreamer() {
        RdfStreamer rdfStreamer =
                idMappingRdfStreamerConfig.idMappingRdfStreamer(properties, rdfServiceFactory);
        assertNotNull(rdfStreamer);
    }

    @Test
    void idMappingRdfRestTemplate() {
        RestTemplate template = idMappingRdfStreamerConfig.idMappingRdfRestTemplate(properties);
        assertNotNull(template);
    }
}