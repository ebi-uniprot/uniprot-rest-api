package org.uniprot.api.common.repository.stream.rdf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.rest.service.RdfService;
import org.uniprot.api.rest.service.TagPositionProvider;

import static org.junit.jupiter.api.Assertions.assertSame;

class RdfServiceFactoryTest {

    public static final String TYPE = "type";
    public static final String FORMAT = "format";
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private TagPositionProvider tagPositionProvider;
    private RdfServiceFactory rdfServiceFactory;

    @BeforeEach
    void setUp() {
        rdfServiceFactory = new RdfServiceFactory(restTemplate, tagPositionProvider);
    }

    @Test
    void getRdfService_whenNotExist() {
        RdfService<String> rdfService = rdfServiceFactory.getRdfService(TYPE, FORMAT);
        assertSame(TYPE, rdfService.getDataType());
        assertSame(FORMAT, rdfService.getFormat());
    }

    @Test
    void getRdfService_whenExist() {
        RdfService<String> rdfServiceFirst = rdfServiceFactory.getRdfService(TYPE, FORMAT);
        RdfService<String> rdfServiceSecond = rdfServiceFactory.getRdfService(TYPE, FORMAT);
        assertSame(rdfServiceFirst, rdfServiceSecond);
    }
}