package org.uniprot.api.uniparc.repository.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.stream.rdf.PrologProvider;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamerConfigProperties;
import org.uniprot.api.rest.service.TagProvider;

import static org.junit.jupiter.api.Assertions.*;

class UniParcRDFStreamerConfigTest {
    @Mock
    private PrologProvider prologProvider;
    @Mock
    private TagProvider tagProvider;
    private UniParcRDFStreamerConfig uniParcRDFStreamerConfig;
    private RDFStreamerConfigProperties properties;

    @BeforeEach
    void setUp() {
        uniParcRDFStreamerConfig = new UniParcRDFStreamerConfig(prologProvider, tagProvider);
        properties = new RDFStreamerConfigProperties();
        properties.setRequestUrl("http://localhost");
        properties.setBatchSize(25);
        properties.setMaxRetries(2);
        properties.setRetryDelayMillis(100);
    }

    @Test
    void uniparcRdfStreamer() {
        RestTemplate restTemplate = new RestTemplate();
        RDFStreamer rdfStreamer = uniParcRDFStreamerConfig.uniparcRdfStreamer(properties, restTemplate);
        assertNotNull(rdfStreamer);
    }

    @Test
    void uniparcRdfRestTemplate() {
        RestTemplate template = uniParcRDFStreamerConfig.uniparcRdfRestTemplate(properties);
        assertNotNull(template);
    }
}