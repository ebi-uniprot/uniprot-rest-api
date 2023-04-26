package org.uniprot.api.idmapping.service.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.stream.rdf.PrologProvider;
import org.uniprot.api.common.repository.stream.rdf.RDFServiceFactory;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamerConfigProperties;
import org.uniprot.api.rest.service.TagProvider;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class IdMappingRDFStreamerConfigTest {
    @Mock
    private PrologProvider prologProvider;
    @Mock
    private TagProvider tagProvider;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private RDFServiceFactory rdfServiceFactory;
    private RDFStreamerConfigProperties properties;
    private IdMappingRDFStreamerConfig idMappingRDFStreamerConfig;

    @BeforeEach
    void setUp() {
        idMappingRDFStreamerConfig = new IdMappingRDFStreamerConfig(prologProvider, tagProvider);
        properties = new RDFStreamerConfigProperties();
        properties.setRequestUrl("http://localhost");
        properties.setBatchSize(25);
        properties.setMaxRetries(2);
        properties.setRetryDelayMillis(100);
    }

    @Test
    void supportDataRdfServiceFactory() {
        RDFServiceFactory rdfServiceFactory = idMappingRDFStreamerConfig.idMappingRdfServiceFactory(restTemplate);
        assertNotNull(rdfServiceFactory);
    }

    @Test
    void idMappingRdfStreamer() {
        RDFStreamer rdfStreamer = idMappingRDFStreamerConfig.idMappingRdfStreamer(properties, rdfServiceFactory);
        assertNotNull(rdfStreamer);
    }

    @Test
    void idMappingRdfRestTemplate() {
        RestTemplate template = idMappingRDFStreamerConfig.idMappingRdfRestTemplate(properties);
        assertNotNull(template);
    }
}