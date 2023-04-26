package org.uniprot.api.uniprotkb.repository.store;

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

class UniProtRDFStreamerConfigTest {
    @Mock
    private PrologProvider prologProvider;
    @Mock
    private TagProvider tagProvider;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private RDFServiceFactory rdfServiceFactory;
    private RDFStreamerConfigProperties properties;
    private UniProtRDFStreamerConfig uniprotRDFStreamerConfig;

    @BeforeEach
    void setUp() {
        uniprotRDFStreamerConfig = new UniProtRDFStreamerConfig(prologProvider, tagProvider);
        properties = new RDFStreamerConfigProperties();
        properties.setRequestUrl("http://localhost");
        properties.setBatchSize(25);
        properties.setMaxRetries(2);
        properties.setRetryDelayMillis(100);
    }

    @Test
    void uniprotRdfStreamer() {
        RDFStreamer rdfStreamer = uniprotRDFStreamerConfig.uniprotRdfStreamer(properties, rdfServiceFactory);
        assertNotNull(rdfStreamer);
    }

    @Test
    void uniprotRdfServiceFactory() {
        RDFServiceFactory rdfServiceFactory = uniprotRDFStreamerConfig.uniprotRdfServiceFactory(restTemplate);
        assertNotNull(rdfServiceFactory);
    }

    @Test
    void uniprotRdfRestTemplate() {
        RestTemplate template = uniprotRDFStreamerConfig.uniprotRdfRestTemplate(properties);
        assertNotNull(template);
    }
}