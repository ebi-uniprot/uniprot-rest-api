package org.uniprot.api.uniref.repository.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.stream.rdf.PrologProvider;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamerConfigProperties;
import org.uniprot.api.rest.service.TagProvider;

import static org.junit.jupiter.api.Assertions.*;

class UniRefRDFStreamerConfigTest {
    @Mock
    private PrologProvider prologProvider;
    @Mock
    private TagProvider tagProvider;
    private UniRefRDFStreamerConfig uniRefRDFStreamerConfig;
    private RDFStreamerConfigProperties properties;

    @BeforeEach
    void setUp() {
        uniRefRDFStreamerConfig = new UniRefRDFStreamerConfig(prologProvider, tagProvider);
        properties = new RDFStreamerConfigProperties();
        properties.setRequestUrl("http://localhost");
        properties.setBatchSize(25);
        properties.setMaxRetries(2);
        properties.setRetryDelayMillis(100);
    }

    @Test
    void uniprotRdfStreamer() {
        RestTemplate restTemplate = new RestTemplate();
        RDFStreamer rdfStreamer = uniRefRDFStreamerConfig.unirefRdfStreamer(properties, restTemplate);
        assertNotNull(rdfStreamer);
    }

    @Test
    void uniprotRdfRestTemplate() {
        RestTemplate template = uniRefRDFStreamerConfig.unirefRdfRestTemplate(properties);
        assertNotNull(template);
    }
}