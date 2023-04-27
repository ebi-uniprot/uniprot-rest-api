package org.uniprot.api.common.repository.stream.rdf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.rest.service.RDFService;
import org.uniprot.api.rest.service.TagPositionProvider;

import static org.junit.jupiter.api.Assertions.assertSame;

class RDFServiceFactoryTest {

    public static final String TYPE = "type";
    public static final String FORMAT = "format";
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private TagPositionProvider tagPositionProvider;
    private RDFServiceFactory rdfServiceFactory;

    @BeforeEach
    void setUp() {
        rdfServiceFactory = new RDFServiceFactory(restTemplate, tagPositionProvider);
    }

    @Test
    void getRdfService_whenNotExist() {
        RDFService<String> rdfService = rdfServiceFactory.getRdfService(TYPE, FORMAT);
        assertSame(TYPE, rdfService.getDataType());
        assertSame(FORMAT, rdfService.getFormat());
    }

    @Test
    void getRdfService_whenExist() {
        RDFService<String> rdfServiceFirst = rdfServiceFactory.getRdfService(TYPE, FORMAT);
        RDFService<String> rdfServiceSecond = rdfServiceFactory.getRdfService(TYPE, FORMAT);
        assertSame(rdfServiceFirst, rdfServiceSecond);
    }
}